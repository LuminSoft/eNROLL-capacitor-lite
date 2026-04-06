package com.luminsoft.enroll.capacitor

import android.util.Log
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.luminsoft.enroll_sdk.core.models.EnrollCallback
import com.luminsoft.enroll_sdk.core.models.EnrollEnvironment
import com.luminsoft.enroll_sdk.core.models.EnrollFailedModel
import com.luminsoft.enroll_sdk.core.models.EnrollForcedDocumentType
import com.luminsoft.enroll_sdk.core.models.EnrollMode
import com.luminsoft.enroll_sdk.core.models.EnrollSuccessModel
import com.luminsoft.enroll_sdk.core.models.LocalizationCode
import com.luminsoft.enroll_sdk.main.main_data.main_models.get_onboaring_configurations.EkycStepType
import com.luminsoft.enroll_sdk.sdk.eNROLL
import com.luminsoft.enroll_sdk.ui_components.theme.AppColors
import androidx.compose.ui.graphics.Color
import org.json.JSONObject

@CapacitorPlugin(name = "Enroll")
class EnrollPlugin : Plugin() {

    companion object {
        private const val TAG = "EnrollPlugin"
    }

    /** Guard against launching a second flow while one is already running. */
    @Volatile
    private var isFlowInProgress = false

    // ------------------------------------------------------------------
    // Plugin method exposed to TypeScript
    // ------------------------------------------------------------------

    @PluginMethod
    fun startEnroll(call: PluginCall) {
        if (isFlowInProgress) {
            call.reject("An enrollment flow is already in progress", "FLOW_IN_PROGRESS")
            return
        }

        val currentActivity = activity
        if (currentActivity == null) {
            call.reject("Activity is not available", "ACTIVITY_ERROR")
            return
        }

        // ---- Required parameters ----
        val tenantId = call.getString("tenantId")
        if (tenantId.isNullOrEmpty()) {
            call.reject("tenantId is required", "INVALID_ARGUMENT")
            return
        }

        val tenantSecret = call.getString("tenantSecret")
        if (tenantSecret.isNullOrEmpty()) {
            call.reject("tenantSecret is required", "INVALID_ARGUMENT")
            return
        }

        val enrollModeStr = call.getString("enrollMode")
        if (enrollModeStr.isNullOrEmpty()) {
            call.reject("enrollMode is required", "INVALID_ARGUMENT")
            return
        }

        val enrollMode = parseEnrollMode(enrollModeStr)
        if (enrollMode == null) {
            call.reject("Invalid enrollMode: $enrollModeStr", "INVALID_ARGUMENT")
            return
        }

        // ---- Conditionally required parameters ----
        val applicantId = call.getString("applicantId") ?: ""
        val levelOfTrust = call.getString("levelOfTrust") ?: ""
        val templateId = call.getString("templateId") ?: ""

        if (enrollMode == EnrollMode.AUTH) {
            if (applicantId.isEmpty()) {
                call.reject("applicantId is required for auth mode", "INVALID_ARGUMENT")
                return
            }
            if (levelOfTrust.isEmpty()) {
                call.reject("levelOfTrust is required for auth mode", "INVALID_ARGUMENT")
                return
            }
        }

        if (enrollMode == EnrollMode.SIGN_CONTRACT) {
            if (templateId.isEmpty()) {
                call.reject("templateId is required for signContract mode", "INVALID_ARGUMENT")
                return
            }
        }

        // ---- Optional parameters ----
        val enrollEnvironment = parseEnrollEnvironment(call.getString("enrollEnvironment"))
        val localizationCode = parseLocalizationCode(call.getString("localizationCode"))
        val googleApiKey = call.getString("googleApiKey") ?: ""
        val skipTutorial = call.getBoolean("skipTutorial", false) ?: false
        val correlationId = call.getString("correlationId") ?: ""
        val requestId = call.getString("requestId") ?: ""
        val contractParameters = call.getString("contractParameters") ?: ""
        val enrollForcedDocumentType = parseEnrollForcedDocumentType(call.getString("enrollForcedDocumentType"))
        val exitStep = parseExitStep(call.getString("enrollExitStep"))

        // ---- Colors ----
        val defaultAppColors = AppColors(
            primary = Color(0xFF1D56B8),
            secondary = Color(0xFF5791DB.toInt()),
            backGround = Color(0xFFFFFFFF),
            textColor = Color(0xFF004194.toInt()),
            errorColor = Color(0xFFDB305B),
            successColor = Color(0xFF61CC3D.toInt()),
            warningColor = Color(0xFFF9D548),
            white = Color(0xFFFFFFFF),
            appBlack = Color(0xFF333333)
        )

        val appColors = call.getObject("enrollColors")?.let { colorsJson ->
            parseEnrollColors(colorsJson, defaultAppColors)
        } ?: defaultAppColors

        // ---- Launch the SDK ----
        isFlowInProgress = true

        try {
            eNROLL.init(
                tenantId,
                tenantSecret,
                applicantId,
                levelOfTrust,
                enrollMode,
                enrollEnvironment,
                localizationCode = localizationCode,
                enrollCallback = object : EnrollCallback {
                    override fun success(enrollSuccessModel: EnrollSuccessModel) {
                        Log.d(TAG, "eNROLL success: ${enrollSuccessModel.enrollMessage}")
                        isFlowInProgress = false

                        val result = JSObject()
                        result.put("applicantId", enrollSuccessModel.applicantId ?: "")
                        result.put("enrollMessage", enrollSuccessModel.enrollMessage)
                        result.put("documentId", enrollSuccessModel.documentId)
                        result.put("requestId", enrollSuccessModel.requestId)
                        result.put("exitStepCompleted", enrollSuccessModel.exitStepCompleted)
                        result.put("completedStepName", enrollSuccessModel.completedStepName)
                        call.resolve(result)
                    }

                    override fun error(enrollFailedModel: EnrollFailedModel) {
                        Log.e(TAG, "eNROLL error: ${enrollFailedModel.failureMessage}")
                        isFlowInProgress = false

                        val errorData = JSObject()
                        errorData.put("message", enrollFailedModel.failureMessage)
                        errorData.put("applicantId", enrollFailedModel.applicantId)
                        call.reject(
                            enrollFailedModel.failureMessage,
                            "ENROLL_ERROR",
                            null,
                            errorData
                        )
                    }

                    override fun getRequestId(rid: String) {
                        Log.d(TAG, "eNROLL requestId: $rid")
                        val data = JSObject()
                        data.put("requestId", rid)
                        notifyListeners("onRequestId", data)
                    }
                },
                googleApiKey = googleApiKey,
                skipTutorial = skipTutorial,
                correlationId = correlationId,
                appColors = appColors,
                enrollForcedDocumentType = enrollForcedDocumentType,
                requestId = requestId,
                templateId = templateId,
                contractParameters = contractParameters,
                exitStep = exitStep
            )

            eNROLL.launch(currentActivity)

        } catch (e: Exception) {
            Log.e(TAG, "Error starting enrollment: ${e.message}", e)
            isFlowInProgress = false
            call.reject("Failed to start enrollment: ${e.message}", "ENROLL_LAUNCH_ERROR")
        }
    }

    // ------------------------------------------------------------------
    // Enum parsers
    // ------------------------------------------------------------------

    private fun parseEnrollMode(mode: String?): EnrollMode? {
        return when (mode) {
            "onboarding" -> EnrollMode.ONBOARDING
            "auth" -> EnrollMode.AUTH
            "update" -> EnrollMode.UPDATE
            "signContract" -> EnrollMode.SIGN_CONTRACT
            "forgetProfileData" -> EnrollMode.FORGET_PROFILE_DATA
            else -> null
        }
    }

    private fun parseEnrollEnvironment(env: String?): EnrollEnvironment {
        return when (env) {
            "production" -> EnrollEnvironment.PRODUCTION
            else -> EnrollEnvironment.STAGING
        }
    }

    private fun parseLocalizationCode(code: String?): LocalizationCode {
        return when (code) {
            "ar" -> LocalizationCode.AR
            else -> LocalizationCode.EN
        }
    }

    private fun parseEnrollForcedDocumentType(type: String?): EnrollForcedDocumentType {
        return when (type) {
            "nationalIdOnly" -> EnrollForcedDocumentType.NATIONAL_ID_ONLY
            "passportOnly" -> EnrollForcedDocumentType.PASSPORT_ONLY
            else -> EnrollForcedDocumentType.NATIONAL_ID_OR_PASSPORT
        }
    }

    private fun parseExitStep(step: String?): EkycStepType? {
        return when (step) {
            "phoneOtp" -> EkycStepType.PhoneOtp
            "personalConfirmation" -> EkycStepType.PersonalConfirmation
            "smileLiveness" -> EkycStepType.SmileLiveness
            "emailOtp" -> EkycStepType.EmailOtp
            "saveMobileDevice" -> EkycStepType.SaveMobileDevice
            "deviceLocation" -> EkycStepType.DeviceLocation
            "password" -> EkycStepType.SettingPassword
            "securityQuestions" -> EkycStepType.SecurityQuestions
            "amlCheck" -> EkycStepType.AmlCheck
            "termsAndConditions" -> EkycStepType.TermsConditions
            "electronicSignature" -> EkycStepType.ElectronicSignature
            "ntraCheck" -> EkycStepType.NtraCheck
            "csoCheck" -> EkycStepType.CsoCheck
            else -> null
        }
    }

    // ------------------------------------------------------------------
    // Color parsing
    // ------------------------------------------------------------------

    private fun parseEnrollColors(colorsJson: JSObject, defaults: AppColors): AppColors {
        return AppColors(
            primary = parseSingleColor(colorsJson.optJSONObject("primary")) ?: defaults.primary,
            secondary = parseSingleColor(colorsJson.optJSONObject("secondary")) ?: defaults.secondary,
            backGround = parseSingleColor(colorsJson.optJSONObject("appBackgroundColor")) ?: defaults.backGround,
            textColor = parseSingleColor(colorsJson.optJSONObject("textColor")) ?: defaults.textColor,
            errorColor = parseSingleColor(colorsJson.optJSONObject("errorColor")) ?: defaults.errorColor,
            successColor = parseSingleColor(colorsJson.optJSONObject("successColor")) ?: defaults.successColor,
            warningColor = parseSingleColor(colorsJson.optJSONObject("warningColor")) ?: defaults.warningColor,
            white = parseSingleColor(colorsJson.optJSONObject("appWhite")) ?: defaults.white,
            appBlack = parseSingleColor(colorsJson.optJSONObject("appBlack")) ?: defaults.appBlack
        )
    }

    private fun parseSingleColor(json: JSONObject?): Color? {
        if (json == null) return null
        val r = json.optInt("r", -1)
        val g = json.optInt("g", -1)
        val b = json.optInt("b", -1)
        if (r == -1 && g == -1 && b == -1) return null
        val opacity = json.optDouble("opacity", 1.0)
        return Color(
            red = (if (r == -1) 0 else r) / 255f,
            green = (if (g == -1) 0 else g) / 255f,
            blue = (if (b == -1) 0 else b) / 255f,
            alpha = opacity.toFloat()
        )
    }
}
