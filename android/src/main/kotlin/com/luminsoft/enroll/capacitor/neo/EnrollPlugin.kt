package com.luminsoft.enroll.capacitor.neo

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
import com.luminsoft.enroll_sdk.ui_components.theme.AppIcons
import com.luminsoft.enroll_sdk.ui_components.theme.AppTheme
import com.luminsoft.enroll_sdk.ui_components.theme.BackgroundIcons
import com.luminsoft.enroll_sdk.ui_components.theme.CommonIcons
import com.luminsoft.enroll_sdk.ui_components.theme.EmailIcons
import com.luminsoft.enroll_sdk.ui_components.theme.FaceMatchingIcons
import com.luminsoft.enroll_sdk.ui_components.theme.FieldIcons
import com.luminsoft.enroll_sdk.ui_components.theme.ForgetIcons
import com.luminsoft.enroll_sdk.ui_components.theme.IconRenderingMode
import com.luminsoft.enroll_sdk.ui_components.theme.IconSource
import com.luminsoft.enroll_sdk.ui_components.theme.LocationIcons
import com.luminsoft.enroll_sdk.ui_components.theme.LogoConfig
import com.luminsoft.enroll_sdk.ui_components.theme.LogoMode
import com.luminsoft.enroll_sdk.ui_components.theme.NationalIdIcons
import com.luminsoft.enroll_sdk.ui_components.theme.PassportIcons
import com.luminsoft.enroll_sdk.ui_components.theme.PasswordIcons
import com.luminsoft.enroll_sdk.ui_components.theme.PhoneIcons
import com.luminsoft.enroll_sdk.ui_components.theme.PopupIcons
import com.luminsoft.enroll_sdk.ui_components.theme.SecurityQuestionsIcons
import com.luminsoft.enroll_sdk.ui_components.theme.SignatureIcons
import com.luminsoft.enroll_sdk.ui_components.theme.StepIcon
import com.luminsoft.enroll_sdk.ui_components.theme.UiIcons
import com.luminsoft.enroll_sdk.ui_components.theme.UpdateIcons
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

        // ---- Theme (colors + icons) ----
        val themeColors = call.getObject("enrollTheme")?.let { themeJson ->
            themeJson.optJSONObject("colors")?.let { colorsJson ->
                parseEnrollColors(JSObject(colorsJson.toString()), defaultAppColors)
            }
        }

        val appColors = themeColors
            ?: call.getObject("enrollColors")?.let { colorsJson ->
                parseEnrollColors(colorsJson, defaultAppColors)
            } ?: defaultAppColors

        val appIcons = call.getObject("enrollTheme")?.let { themeJson ->
            themeJson.optJSONObject("icons")?.let { iconsJson ->
                parseAppIcons(iconsJson)
            }
        } ?: AppIcons()

        val appTheme = AppTheme(
            colors = appColors,
            icons = appIcons
        )

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
                appTheme = appTheme,
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

    // ------------------------------------------------------------------
    // Theme / icon parsing
    // ------------------------------------------------------------------

    private fun resolveDrawableName(name: String): Int {
        val ctx = context ?: return 0
        val resId = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
        if (resId == 0) Log.w(TAG, "Drawable not found: $name")
        return resId
    }

    private fun parseStepIcon(json: JSONObject): StepIcon? {
        val assetName = json.optString("assetName", "").takeIf { it.isNotEmpty() } ?: return null
        val resId = resolveDrawableName(assetName)
        if (resId == 0) return null
        val renderingMode = when (json.optString("renderingMode")) {
            "template" -> IconRenderingMode.TEMPLATE
            else -> IconRenderingMode.ORIGINAL
        }
        return StepIcon(source = IconSource.Resource(resId), renderingMode = renderingMode)
    }

    private fun parseLogoConfig(json: JSONObject): LogoConfig {
        val mode = when (json.optString("mode")) {
            "custom" -> LogoMode.CUSTOM
            "hidden" -> LogoMode.HIDDEN
            else -> LogoMode.DEFAULT
        }
        val asset = json.optString("assetName", "").takeIf { it.isNotEmpty() }?.let {
            val resId = resolveDrawableName(it)
            if (resId != 0) IconSource.Resource(resId) else null
        }
        val renderingMode = when (json.optString("renderingMode")) {
            "template" -> IconRenderingMode.TEMPLATE
            else -> IconRenderingMode.ORIGINAL
        }
        val showSponsoredBy = json.optBoolean("showSponsoredBy", true)
        return LogoConfig(mode = mode, asset = asset, renderingMode = renderingMode, showSponsoredBy = showSponsoredBy)
    }

    private fun stepIconFromParent(parent: JSONObject, key: String): StepIcon? {
        return parent.optJSONObject(key)?.let { parseStepIcon(it) }
    }

    private fun parseAppIcons(json: JSONObject): AppIcons {
        return AppIcons(
            logo = json.optJSONObject("logo")?.let { parseLogoConfig(it) } ?: LogoConfig(),
            location = json.optJSONObject("location")?.let { parseLocationIcons(it) } ?: LocationIcons(),
            nationalId = json.optJSONObject("nationalId")?.let { parseNationalIdIcons(it) } ?: NationalIdIcons(),
            passport = json.optJSONObject("passport")?.let { parsePassportIcons(it) } ?: PassportIcons(),
            phone = json.optJSONObject("phone")?.let { parsePhoneIcons(it) } ?: PhoneIcons(),
            email = json.optJSONObject("email")?.let { parseEmailIcons(it) } ?: EmailIcons(),
            faceMatching = json.optJSONObject("faceMatching")?.let { parseFaceMatchingIcons(it) } ?: FaceMatchingIcons(),
            securityQuestions = json.optJSONObject("securityQuestions")?.let { parseSecurityQuestionsIcons(it) } ?: SecurityQuestionsIcons(),
            password = json.optJSONObject("password")?.let { parsePasswordIcons(it) } ?: PasswordIcons(),
            signature = json.optJSONObject("signature")?.let { parseSignatureIcons(it) } ?: SignatureIcons(),
            common = json.optJSONObject("common")?.let { parseCommonIcons(it) } ?: CommonIcons(),
            update = json.optJSONObject("update")?.let { parseUpdateIcons(it) } ?: UpdateIcons(),
            forget = json.optJSONObject("forget")?.let { parseForgetIcons(it) } ?: ForgetIcons(),
        )
    }

    private fun parseLocationIcons(j: JSONObject) = LocationIcons(
        tutorial = stepIconFromParent(j, "tutorial"), requestAccess = stepIconFromParent(j, "requestAccess"),
        accessError = stepIconFromParent(j, "accessError"), grab = stepIconFromParent(j, "grab"),
    )
    private fun parseNationalIdIcons(j: JSONObject) = NationalIdIcons(
        tutorial = stepIconFromParent(j, "tutorial"), tutorialIdOrPassport = stepIconFromParent(j, "tutorialIdOrPassport"),
        preScan = stepIconFromParent(j, "preScan"), scanError = stepIconFromParent(j, "scanError"), choose = stepIconFromParent(j, "choose"),
    )
    private fun parsePassportIcons(j: JSONObject) = PassportIcons(
        tutorial = stepIconFromParent(j, "tutorial"), preScan = stepIconFromParent(j, "preScan"),
        ePassportPreScan = stepIconFromParent(j, "ePassportPreScan"), choose = stepIconFromParent(j, "choose"),
    )
    private fun parsePhoneIcons(j: JSONObject) = PhoneIcons(
        tutorial = stepIconFromParent(j, "tutorial"), select = stepIconFromParent(j, "select"), validateOtp = stepIconFromParent(j, "validateOtp"),
    )
    private fun parseEmailIcons(j: JSONObject) = EmailIcons(
        tutorial = stepIconFromParent(j, "tutorial"), select = stepIconFromParent(j, "select"), validateOtp = stepIconFromParent(j, "validateOtp"),
    )
    private fun parseFaceMatchingIcons(j: JSONObject) = FaceMatchingIcons(
        tutorial = stepIconFromParent(j, "tutorial"), preScan = stepIconFromParent(j, "preScan"), error = stepIconFromParent(j, "error"),
    )
    private fun parseSecurityQuestionsIcons(j: JSONObject) = SecurityQuestionsIcons(
        tutorial = stepIconFromParent(j, "tutorial"), authScreen = stepIconFromParent(j, "authScreen"),
    )
    private fun parsePasswordIcons(j: JSONObject) = PasswordIcons(
        tutorial = stepIconFromParent(j, "tutorial"), authScreen = stepIconFromParent(j, "authScreen"),
    )
    private fun parseSignatureIcons(j: JSONObject) = SignatureIcons(
        tutorial = stepIconFromParent(j, "tutorial"),
    )
    private fun parseCommonIcons(j: JSONObject) = CommonIcons(
        backgrounds = j.optJSONObject("backgrounds")?.let { parseBackgroundIcons(it) } ?: BackgroundIcons(),
        popups = j.optJSONObject("popups")?.let { parsePopupIcons(it) } ?: PopupIcons(),
        fieldIcons = j.optJSONObject("fieldIcons")?.let { parseFieldIcons(it) } ?: FieldIcons(),
        ui = j.optJSONObject("ui")?.let { parseUiIcons(it) } ?: UiIcons(),
        termsAndConditions = stepIconFromParent(j, "termsAndConditions"),
    )
    private fun parseBackgroundIcons(j: JSONObject) = BackgroundIcons(
        main = stepIconFromParent(j, "main"), layer1 = stepIconFromParent(j, "layer1"),
        layer2 = stepIconFromParent(j, "layer2"), layer3 = stepIconFromParent(j, "layer3"),
        blur = stepIconFromParent(j, "blur"), header = stepIconFromParent(j, "header"), footer = stepIconFromParent(j, "footer"),
    )
    private fun parsePopupIcons(j: JSONObject) = PopupIcons(
        background = stepIconFromParent(j, "background"), warningIcon = stepIconFromParent(j, "warningIcon"),
        errorIcon = stepIconFromParent(j, "errorIcon"), successIcon = stepIconFromParent(j, "successIcon"),
    )
    private fun parseFieldIcons(j: JSONObject) = FieldIcons(
        user = stepIconFromParent(j, "user"), calendar = stepIconFromParent(j, "calendar"),
        gender = stepIconFromParent(j, "gender"), issuingAuthority = stepIconFromParent(j, "issuingAuthority"),
        nationality = stepIconFromParent(j, "nationality"), num = stepIconFromParent(j, "num"),
        passport = stepIconFromParent(j, "passport"), address = stepIconFromParent(j, "address"),
        idCard = stepIconFromParent(j, "idCard"), profession = stepIconFromParent(j, "profession"),
        religion = stepIconFromParent(j, "religion"), maritalStatus = stepIconFromParent(j, "maritalStatus"),
    )
    private fun parseUiIcons(j: JSONObject) = UiIcons(
        visibility = stepIconFromParent(j, "visibility"), visibilityOff = stepIconFromParent(j, "visibilityOff"),
        mobile = stepIconFromParent(j, "mobile"), mail = stepIconFromParent(j, "mail"),
        answer = stepIconFromParent(j, "answer"), error = stepIconFromParent(j, "error"),
        info = stepIconFromParent(j, "info"), edit = stepIconFromParent(j, "edit"), activePhone = stepIconFromParent(j, "activePhone"),
    )
    private fun parseUpdateIcons(j: JSONObject) = UpdateIcons(
        modeIcon = stepIconFromParent(j, "modeIcon"), idCard = stepIconFromParent(j, "idCard"),
        passport = stepIconFromParent(j, "passport"), mobile = stepIconFromParent(j, "mobile"),
        email = stepIconFromParent(j, "email"), device = stepIconFromParent(j, "device"),
        address = stepIconFromParent(j, "address"), securityQuestions = stepIconFromParent(j, "securityQuestions"),
        password = stepIconFromParent(j, "password"),
    )
    private fun parseForgetIcons(j: JSONObject) = ForgetIcons(
        modeIcon = stepIconFromParent(j, "modeIcon"), nationalId = stepIconFromParent(j, "nationalId"),
        passport = stepIconFromParent(j, "passport"), phone = stepIconFromParent(j, "phone"),
        email = stepIconFromParent(j, "email"), device = stepIconFromParent(j, "device"),
        location = stepIconFromParent(j, "location"), securityQuestions = stepIconFromParent(j, "securityQuestions"),
        password = stepIconFromParent(j, "password"),
    )
}
