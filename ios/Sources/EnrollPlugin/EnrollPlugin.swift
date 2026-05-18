import Foundation
import Capacitor
import UIKit
import EnrollFramework

@objc(EnrollPlugin)
public class EnrollPlugin: CAPPlugin, CAPBridgedPlugin, EnrollCallBack {

    public let identifier = "EnrollPlugin"
    public let jsName = "Enroll"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "startEnroll", returnType: CAPPluginReturnPromise)
    ]

    /// Guard against launching a second flow while one is already running.
    private var isFlowInProgress = false

    /// Saved reference to the current PluginCall so callbacks can resolve/reject it.
    private var savedCall: CAPPluginCall?

    // ------------------------------------------------------------------
    // MARK: - Plugin method exposed to TypeScript
    // ------------------------------------------------------------------

    @objc func startEnroll(_ call: CAPPluginCall) {
        if isFlowInProgress {
            call.reject("An enrollment flow is already in progress", "FLOW_IN_PROGRESS")
            return
        }

        // ---- Required parameters ----
        guard let tenantId = call.getString("tenantId"), !tenantId.isEmpty else {
            call.reject("tenantId is required", "INVALID_ARGUMENT")
            return
        }
        guard let tenantSecret = call.getString("tenantSecret"), !tenantSecret.isEmpty else {
            call.reject("tenantSecret is required", "INVALID_ARGUMENT")
            return
        }
        guard let enrollModeStr = call.getString("enrollMode"), !enrollModeStr.isEmpty else {
            call.reject("enrollMode is required", "INVALID_ARGUMENT")
            return
        }
        guard let enrollMode = parseEnrollMode(enrollModeStr) else {
            call.reject("Invalid enrollMode: \(enrollModeStr)", "INVALID_ARGUMENT")
            return
        }

        // ---- Conditionally required parameters ----
        let applicantId = call.getString("applicantId") ?? ""
        let levelOfTrust = call.getString("levelOfTrust") ?? ""
        let templateId = call.getString("templateId") ?? ""

        if enrollMode == .authentication {
            if applicantId.isEmpty {
                call.reject("applicantId is required for auth mode", "INVALID_ARGUMENT")
                return
            }
            if levelOfTrust.isEmpty {
                call.reject("levelOfTrust is required for auth mode", "INVALID_ARGUMENT")
                return
            }
        }

        if enrollMode == .signContarct {
            if templateId.isEmpty {
                call.reject("templateId is required for signContract mode", "INVALID_ARGUMENT")
                return
            }
        }

        // ---- Optional parameters ----
        let enrollEnvironment = parseEnrollEnvironment(call.getString("enrollEnvironment"))
        let localizationCode = parseLocalizationCode(call.getString("localizationCode"))
        let googleApiKey = call.getString("googleApiKey") ?? ""
        let skipTutorial = call.getBool("skipTutorial") ?? false
        let correlationId = call.getString("correlationId") ?? ""
        let requestId = call.getString("requestId") ?? ""
        let contractParameters = call.getString("contractParameters") ?? ""
        let enrollForcedDocumentType = parseEnrollForcedDocumentType(call.getString("enrollForcedDocumentType"))
        let exitStep = parseExitStep(call.getString("enrollExitStep"))
        let contractTemplateId = Int(templateId)

        // ---- Colors ----
        let enrollColors: EnrollColors? = {
            guard let colorsObj = call.getObject("enrollColors") else { return nil }
            return generateDynamicColors(colors: colorsObj)
        }()

        // ---- Theme (colors + icons) ----
        let enrollTheme: EnrollTheme? = {
            guard let themeObj = call.getObject("enrollTheme") else { return nil }
            return self.generateDynamicTheme(theme: themeObj)
        }()

        // ---- RTL layout for Arabic ----
        configureLayoutDirection(localizationCode)

        // ---- Save call & mark in progress ----
        self.savedCall = call
        self.isFlowInProgress = true

        // ---- Launch SDK on main thread ----
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            guard let presenterVC = self.bridge?.viewController else {
                self.isFlowInProgress = false
                self.savedCall = nil
                call.reject("Unable to get presenting view controller", "VIEW_CONTROLLER_ERROR")
                return
            }

            do {
                let initModel = try EnrollInitModel(
                    tenantId: tenantId,
                    tenantSecret: tenantSecret,
                    enrollEnviroment: enrollEnvironment,
                    localizationCode: localizationCode,
                    enrollCallBack: self,
                    enrollMode: enrollMode,
                    skipTutorial: skipTutorial,
                    enrollColors: enrollColors,
                    enrollTheme: enrollTheme,
                    levelOffTrustId: levelOfTrust.isEmpty ? nil : levelOfTrust,
                    applicantId: applicantId.isEmpty ? nil : applicantId,
                    correlationId: correlationId.isEmpty ? nil : correlationId,
                    forcedDocumentType: enrollForcedDocumentType,
                    requestId: requestId.isEmpty ? nil : requestId,
                    contractTemplateId: contractTemplateId,
                    signContarctParam: contractParameters.isEmpty ? nil : contractParameters,
                    exitStep: exitStep
                )

                let enrollVC = try Enroll.initViewController(
                    enrollInitModel: initModel,
                    presenterVC: presenterVC
                )
                presenterVC.present(enrollVC, animated: true)
            } catch {
                self.isFlowInProgress = false
                self.savedCall = nil
                call.reject("Failed to start enrollment: \(error.localizedDescription)", "ENROLL_LAUNCH_ERROR")
            }
        }
    }

    // ------------------------------------------------------------------
    // MARK: - EnrollCallBack protocol
    // ------------------------------------------------------------------

    public func enrollDidSucceed(with model: EnrollFramework.EnrollSuccessModel) {
        isFlowInProgress = false
        guard let call = savedCall else { return }
        savedCall = nil

        var result: [String: Any] = [
            "applicantId": model.applicantId ?? "",
            "exitStepCompleted": false
        ]
        call.resolve(result)
    }

    public func enrollDidFail(with error: EnrollFramework.EnrollErrorModel) {
        isFlowInProgress = false
        guard let call = savedCall else { return }
        savedCall = nil

        call.reject(error.errorMessage ?? "Unknown error", "ENROLL_ERROR")
    }

    public func didInitializeRequest(with requestId: String) {
        notifyListeners("onRequestId", data: ["requestId": requestId])
    }

    // ------------------------------------------------------------------
    // MARK: - Enum parsers
    // ------------------------------------------------------------------

    private func parseEnrollMode(_ mode: String) -> EnrollMode? {
        switch mode.lowercased() {
        case "onboarding":
            return .onboarding
        case "auth":
            return .authentication
        case "update":
            return .update
        case "signcontract":
            return .signContarct
        case "forgetprofiledata":
            return .forget
        default:
            return nil
        }
    }

    private func parseEnrollEnvironment(_ env: String?) -> EnrollFramework.EnrollEnviroment {
        switch env {
        case "production":
            return .production
        default:
            return .staging
        }
    }

    private func parseLocalizationCode(_ code: String?) -> EnrollFramework.LocalizationEnum {
        switch code {
        case "ar":
            return .ar
        default:
            return .en
        }
    }

    private func parseEnrollForcedDocumentType(_ type: String?) -> EnrollForcedDocumentType? {
        switch type {
        case "nationalIdOnly":
            return .nationalId
        case "passportOnly":
            return .passport
        case "nationalIdOrPassport":
            return .deafult
        default:
            return nil
        }
    }

    private func parseExitStep(_ step: String?) -> EnrollFramework.StepType? {
        guard let step = step else { return nil }
        switch step {
        case "phoneOtp":
            return .phoneOtp
        case "personalConfirmation":
            return .personalConfirmation
        case "smileLiveness":
            return .smileLiveness
        case "emailOtp":
            return .emailOtp
        case "saveMobileDevice":
            return .saveMobileDevice
        case "deviceLocation":
            return .deviceLocation
        case "password":
            return .password
        case "securityQuestions":
            return .securityQuestions
        case "amlCheck":
            return .amlCheck
        case "termsAndConditions":
            return .termsAndConditions
        case "electronicSignature":
            return .electronicSignature
        case "ntraCheck":
            return .ntraCheck
        case "csoCheck":
            return .csoCheck
        default:
            return nil
        }
    }

    // ------------------------------------------------------------------
    // MARK: - Color parsing
    // ------------------------------------------------------------------

    private func generateDynamicColors(colors: [String: Any]) -> EnrollColors? {
        var primaryColor: UIColor?
        var appBackgroundColor: UIColor?
        var appBlack: UIColor?
        var secondary: UIColor?
        var appWhite: UIColor?
        var errorColor: UIColor?
        var textColor: UIColor?
        var successColor: UIColor?
        var warningColor: UIColor?

        if let primary = colors["primary"] as? [String: Any] {
            primaryColor = uiColorFrom(dict: primary)
        }
        if let bg = colors["appBackgroundColor"] as? [String: Any] {
            appBackgroundColor = uiColorFrom(dict: bg)
        }
        if let black = colors["appBlack"] as? [String: Any] {
            appBlack = uiColorFrom(dict: black)
        }
        if let sec = colors["secondary"] as? [String: Any] {
            secondary = uiColorFrom(dict: sec)
        }
        if let white = colors["appWhite"] as? [String: Any] {
            appWhite = uiColorFrom(dict: white)
        }
        if let err = colors["errorColor"] as? [String: Any] {
            errorColor = uiColorFrom(dict: err)
        }
        if let txt = colors["textColor"] as? [String: Any] {
            textColor = uiColorFrom(dict: txt)
        }
        if let suc = colors["successColor"] as? [String: Any] {
            successColor = uiColorFrom(dict: suc)
        }
        if let warn = colors["warningColor"] as? [String: Any] {
            warningColor = uiColorFrom(dict: warn)
        }

        return EnrollColors(
            primary: primaryColor,
            secondary: secondary,
            appBackgroundColor: appBackgroundColor,
            textColor: textColor,
            errorColor: errorColor,
            successColor: successColor,
            warningColor: warningColor,
            appWhite: appWhite,
            appBlack: appBlack
        )
    }

    private func uiColorFrom(dict: [String: Any]) -> UIColor? {
        guard let r = dict["r"] as? Int,
              let g = dict["g"] as? Int,
              let b = dict["b"] as? Int else {
            return nil
        }
        let opacity = dict["opacity"] as? Double ?? 1.0
        return UIColor(
            red: CGFloat(r) / 255.0,
            green: CGFloat(g) / 255.0,
            blue: CGFloat(b) / 255.0,
            alpha: CGFloat(opacity)
        )
    }

    // ------------------------------------------------------------------
    // MARK: - Theme / icon parsing
    // ------------------------------------------------------------------

    private func generateDynamicTheme(theme: [String: Any]?) -> EnrollTheme {
        guard let theme = theme else { return EnrollTheme() }
        var enrollColors: EnrollColors?
        if let colorDict = theme["colors"] as? [String: Any] {
            enrollColors = generateDynamicColors(colors: colorDict)
        }
        var appIcons = AppIcons()
        if let iconsDict = theme["icons"] as? [String: Any] {
            appIcons = generateAppIcons(from: iconsDict)
        }
        return EnrollTheme(icons: appIcons, colors: enrollColors)
    }

    private func generateAppIcons(from dictionary: [String: Any]) -> AppIcons {
        var logo = LogoConfig()
        var location = LocationIcons()
        var nationalId: NationalIdIcons?
        var passport = PassportIcons()
        var phone = PhoneIcons()
        var email = EmailIcons()
        var faceMatching = FaceMatchingIcons()
        var securityQuestions = SecurityQuestionsIcons()
        var password = PasswordIcons()
        var signature = SignatureIcons()
        var common = CommonIcons()
        var update = UpdateIcons()
        var forget = ForgetIcons()

        if let logoDict = dictionary["logo"] as? [String: Any] { logo = parseLogoConfig(from: logoDict) }
        if let locationDict = dictionary["location"] as? [String: Any] { location = parseLocationIcons(from: locationDict) }
        if let nationalIdDict = dictionary["nationalId"] as? [String: Any] { nationalId = parseNationalIdIcons(from: nationalIdDict) }
        if let passportDict = dictionary["passport"] as? [String: Any] { passport = parsePassportIcons(from: passportDict) }
        if let phoneDict = dictionary["phone"] as? [String: Any] { phone = parsePhoneIcons(from: phoneDict) }
        if let emailDict = dictionary["email"] as? [String: Any] { email = parseEmailIcons(from: emailDict) }
        if let fmDict = dictionary["faceMatching"] as? [String: Any] { faceMatching = parseFaceMatchingIcons(from: fmDict) }
        if let sqDict = dictionary["securityQuestions"] as? [String: Any] { securityQuestions = parseSecurityQuestionsIcons(from: sqDict) }
        if let pwDict = dictionary["password"] as? [String: Any] { password = parsePasswordIcons(from: pwDict) }
        if let sigDict = dictionary["signature"] as? [String: Any] { signature = parseSignatureIcons(from: sigDict) }
        if let comDict = dictionary["common"] as? [String: Any] { common = parseCommonIcons(from: comDict) }
        if let updDict = dictionary["update"] as? [String: Any] { update = parseUpdateIcons(from: updDict) }
        if let fgtDict = dictionary["forget"] as? [String: Any] { forget = parseForgetIcons(from: fgtDict) }

        return AppIcons(logo: logo, location: location, nationalId: nationalId, passport: passport, phone: phone, email: email, faceMatching: faceMatching, securityQuestions: securityQuestions, password: password, signature: signature, common: common, update: update, forget: forget)
    }

    private func parseLogoConfig(from dictionary: [String: Any]) -> LogoConfig {
        var mode: LogoMode = .default
        var icon: EnrollIcon?
        var showSponsoredBy: Bool = true
        if let modeString = dictionary["mode"] as? String {
            switch modeString.lowercased() {
            case "hidden": mode = .hidden
            case "custom": mode = .custom
            default: mode = .default
            }
        }
        if let _ = dictionary["assetName"] as? String { icon = parseEnrollIcon(from: dictionary) }
        if let val_ = dictionary["showSponsoredBy"] as? Bool { showSponsoredBy = val_ }
        return LogoConfig(mode: mode, icon: icon, showSponsoredBy: showSponsoredBy)
    }

    private func parseEnrollIcon(from dictionary: [String: Any]) -> EnrollIcon {
        let assetName = dictionary["assetName"] as? String ?? ""
        var renderingMode: EnrollIconRenderingMode = .original
        if let rm = dictionary["renderingMode"] as? String { renderingMode = rm.lowercased() == "template" ? .template : .original }
        var validationMode: IconValidationMode = .relaxed
        if let vm = dictionary["validationMode"] as? String { validationMode = vm.lowercased() == "strict" ? .strict : .relaxed }
        return EnrollIcon(assetName: assetName, renderingMode: renderingMode, bundle: Bundle.main, validationMode: validationMode)
    }

    private func parseStepIcon(from dictionary: [String: Any]) -> StepIcon? {
        let enrollIcon = parseEnrollIcon(from: dictionary)
        return StepIcon(icon: enrollIcon)
    }

    private func parseLocationIcons(from d: [String: Any]) -> LocationIcons {
        return LocationIcons(tutorial: parseStepIcon(from: d["tutorial"] as? [String: Any] ?? [:]), requestAccess: parseStepIcon(from: d["requestAccess"] as? [String: Any] ?? [:]), accessError: parseStepIcon(from: d["accessError"] as? [String: Any] ?? [:]), grab: parseStepIcon(from: d["grab"] as? [String: Any] ?? [:]))
    }
    private func parseNationalIdIcons(from d: [String: Any]) -> NationalIdIcons {
        return NationalIdIcons(tutorial: parseStepIcon(from: d["tutorial"] as? [String: Any] ?? [:]), tutorialIdOrPassport: parseStepIcon(from: d["tutorialIdOrPassport"] as? [String: Any] ?? [:]), preScan: parseStepIcon(from: d["preScan"] as? [String: Any] ?? [:]), scanError: parseStepIcon(from: d["scanError"] as? [String: Any] ?? [:]), choose: parseStepIcon(from: d["choose"] as? [String: Any] ?? [:]))
    }
    private func parsePassportIcons(from d: [String: Any]) -> PassportIcons {
        return PassportIcons(tutorial: parseStepIcon(from: d["tutorial"] as? [String: Any] ?? [:]), preScan: parseStepIcon(from: d["preScan"] as? [String: Any] ?? [:]), ePassportPreScan: parseStepIcon(from: d["ePassportPreScan"] as? [String: Any] ?? [:]), choose: parseStepIcon(from: d["choose"] as? [String: Any] ?? [:]), scanError: parseStepIcon(from: d["scanError"] as? [String: Any] ?? [:]))
    }
    private func parsePhoneIcons(from d: [String: Any]) -> PhoneIcons {
        return PhoneIcons(tutorial: parseStepIcon(from: d["tutorial"] as? [String: Any] ?? [:]), select: parseStepIcon(from: d["select"] as? [String: Any] ?? [:]), validateOtp: parseStepIcon(from: d["validateOtp"] as? [String: Any] ?? [:]))
    }
    private func parseEmailIcons(from d: [String: Any]) -> EmailIcons {
        return EmailIcons(tutorial: parseStepIcon(from: d["tutorial"] as? [String: Any] ?? [:]), select: parseStepIcon(from: d["select"] as? [String: Any] ?? [:]), validateOtp: parseStepIcon(from: d["validateOtp"] as? [String: Any] ?? [:]))
    }
    private func parseFaceMatchingIcons(from d: [String: Any]) -> FaceMatchingIcons {
        return FaceMatchingIcons(tutorial: parseStepIcon(from: d["tutorial"] as? [String: Any] ?? [:]), preScan: parseStepIcon(from: d["preScan"] as? [String: Any] ?? [:]), error: parseStepIcon(from: d["error"] as? [String: Any] ?? [:]))
    }
    private func parseSecurityQuestionsIcons(from d: [String: Any]) -> SecurityQuestionsIcons {
        return SecurityQuestionsIcons(tutorial: parseStepIcon(from: d["tutorial"] as? [String: Any] ?? [:]), authScreen: parseStepIcon(from: d["authScreen"] as? [String: Any] ?? [:]))
    }
    private func parsePasswordIcons(from d: [String: Any]) -> PasswordIcons {
        return PasswordIcons(tutorial: parseStepIcon(from: d["tutorial"] as? [String: Any] ?? [:]), authScreen: parseStepIcon(from: d["authScreen"] as? [String: Any] ?? [:]))
    }
    private func parseSignatureIcons(from d: [String: Any]) -> SignatureIcons {
        return SignatureIcons(tutorial: parseStepIcon(from: d["tutorial"] as? [String: Any] ?? [:]))
    }
    private func parseCommonIcons(from d: [String: Any]) -> CommonIcons {
        return CommonIcons(backgrounds: parseBackgroundIcons(from: d["backgrounds"] as? [String: Any] ?? [:]), popups: parsePopupIcons(from: d["popups"] as? [String: Any] ?? [:]), fieldIcons: parseFieldIcons(from: d["fieldIcons"] as? [String: Any] ?? [:]), ui: parseUiIcons(from: d["ui"] as? [String: Any] ?? [:]), termsAndConditions: parseStepIcon(from: d["termsAndConditions"] as? [String: Any] ?? [:]))
    }
    private func parseBackgroundIcons(from d: [String: Any]) -> BackgroundIcons {
        return BackgroundIcons(main: parseStepIcon(from: d["main"] as? [String: Any] ?? [:]), layer1: parseStepIcon(from: d["layer1"] as? [String: Any] ?? [:]), layer2: parseStepIcon(from: d["layer2"] as? [String: Any] ?? [:]), layer3: parseStepIcon(from: d["layer3"] as? [String: Any] ?? [:]), blur: parseStepIcon(from: d["blur"] as? [String: Any] ?? [:]), header: parseStepIcon(from: d["header"] as? [String: Any] ?? [:]), footer: parseStepIcon(from: d["footer"] as? [String: Any] ?? [:]))
    }
    private func parsePopupIcons(from d: [String: Any]) -> PopupIcons {
        return PopupIcons(background: parseStepIcon(from: d["background"] as? [String: Any] ?? [:]), warningIcon: parseStepIcon(from: d["warningIcon"] as? [String: Any] ?? [:]), errorIcon: parseStepIcon(from: d["errorIcon"] as? [String: Any] ?? [:]), successIcon: parseStepIcon(from: d["successIcon"] as? [String: Any] ?? [:]))
    }
    private func parseFieldIcons(from d: [String: Any]) -> FieldIcons {
        return FieldIcons(user: parseStepIcon(from: d["user"] as? [String: Any] ?? [:]), calendar: parseStepIcon(from: d["calendar"] as? [String: Any] ?? [:]), gender: parseStepIcon(from: d["gender"] as? [String: Any] ?? [:]), issuingAuthority: parseStepIcon(from: d["issuingAuthority"] as? [String: Any] ?? [:]), nationality: parseStepIcon(from: d["nationality"] as? [String: Any] ?? [:]), num: parseStepIcon(from: d["num"] as? [String: Any] ?? [:]), passport: parseStepIcon(from: d["passport"] as? [String: Any] ?? [:]), address: parseStepIcon(from: d["address"] as? [String: Any] ?? [:]), idCard: parseStepIcon(from: d["idCard"] as? [String: Any] ?? [:]), profession: parseStepIcon(from: d["profession"] as? [String: Any] ?? [:]), religion: parseStepIcon(from: d["religion"] as? [String: Any] ?? [:]), maritalStatus: parseStepIcon(from: d["maritalStatus"] as? [String: Any] ?? [:]))
    }
    private func parseUiIcons(from d: [String: Any]) -> UiIcons {
        return UiIcons(visibility: parseStepIcon(from: d["visibility"] as? [String: Any] ?? [:]), visibilityOff: parseStepIcon(from: d["visibilityOff"] as? [String: Any] ?? [:]), mobile: parseStepIcon(from: d["mobile"] as? [String: Any] ?? [:]), mail: parseStepIcon(from: d["mail"] as? [String: Any] ?? [:]), answer: parseStepIcon(from: d["answer"] as? [String: Any] ?? [:]), error: parseStepIcon(from: d["error"] as? [String: Any] ?? [:]), info: parseStepIcon(from: d["info"] as? [String: Any] ?? [:]), edit: parseStepIcon(from: d["edit"] as? [String: Any] ?? [:]), activePhone: parseStepIcon(from: d["activePhone"] as? [String: Any] ?? [:]))
    }
    private func parseUpdateIcons(from d: [String: Any]) -> UpdateIcons {
        return UpdateIcons(modeIcon: parseStepIcon(from: d["modeIcon"] as? [String: Any] ?? [:]), idCard: parseStepIcon(from: d["idCard"] as? [String: Any] ?? [:]), passport: parseStepIcon(from: d["passport"] as? [String: Any] ?? [:]), mobile: parseStepIcon(from: d["mobile"] as? [String: Any] ?? [:]), email: parseStepIcon(from: d["email"] as? [String: Any] ?? [:]), device: parseStepIcon(from: d["device"] as? [String: Any] ?? [:]), address: parseStepIcon(from: d["address"] as? [String: Any] ?? [:]), securityQuestions: parseStepIcon(from: d["securityQuestions"] as? [String: Any] ?? [:]), password: parseStepIcon(from: d["password"] as? [String: Any] ?? [:]))
    }
    private func parseForgetIcons(from d: [String: Any]) -> ForgetIcons {
        return ForgetIcons(modeIcon: parseStepIcon(from: d["modeIcon"] as? [String: Any] ?? [:]), nationalId: parseStepIcon(from: d["nationalId"] as? [String: Any] ?? [:]), passport: parseStepIcon(from: d["passport"] as? [String: Any] ?? [:]), phone: parseStepIcon(from: d["phone"] as? [String: Any] ?? [:]), email: parseStepIcon(from: d["email"] as? [String: Any] ?? [:]), device: parseStepIcon(from: d["device"] as? [String: Any] ?? [:]), location: parseStepIcon(from: d["location"] as? [String: Any] ?? [:]), securityQuestions: parseStepIcon(from: d["securityQuestions"] as? [String: Any] ?? [:]), password: parseStepIcon(from: d["password"] as? [String: Any] ?? [:]))
    }

    // ------------------------------------------------------------------
    // MARK: - RTL layout configuration
    // ------------------------------------------------------------------

    private func configureLayoutDirection(_ code: EnrollFramework.LocalizationEnum) {
        DispatchQueue.main.async {
            if code == .ar {
                UIView.appearance().semanticContentAttribute = .forceRightToLeft
                UICollectionView.appearance().semanticContentAttribute = .forceRightToLeft
                UINavigationBar.appearance().semanticContentAttribute = .forceRightToLeft
                UITextField.appearance().semanticContentAttribute = .forceRightToLeft
                UITextField.appearance().textAlignment = .right
                UITextView.appearance().semanticContentAttribute = .forceRightToLeft
                UITableView.appearance().semanticContentAttribute = .forceRightToLeft
            } else {
                UIView.appearance().semanticContentAttribute = .forceLeftToRight
                UICollectionView.appearance().semanticContentAttribute = .forceLeftToRight
                UINavigationBar.appearance().semanticContentAttribute = .forceLeftToRight
                UITextField.appearance().semanticContentAttribute = .forceLeftToRight
                UITextField.appearance().textAlignment = .left
                UITextView.appearance().semanticContentAttribute = .forceLeftToRight
                UITableView.appearance().semanticContentAttribute = .forceLeftToRight
            }
        }
    }
}
