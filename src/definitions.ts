import type { PluginListenerHandle } from '@capacitor/core';

// ---------------------------------------------------------------------------
// Enums (string literal unions — idiomatic TypeScript, no runtime overhead)
// ---------------------------------------------------------------------------

/**
 * The environment in which the eNROLL SDK operates.
 * - `'staging'`    — test / QA environment
 * - `'production'` — live environment
 */
export type EnrollEnvironment = 'staging' | 'production';

/**
 * The mode of the enrollment flow.
 * - `'onboarding'`        — register a new user
 * - `'auth'`              — authenticate an existing user (requires `applicantId` + `levelOfTrust`)
 * - `'update'`            — re-verify / update an existing user
 * - `'signContract'`      — sign a contract template (requires `templateId`)
 * - `'forgetProfileData'` — request deletion of a user's profile data
 */
export type EnrollMode =
  | 'onboarding'
  | 'auth'
  | 'update'
  | 'signContract'
  | 'forgetProfileData';

/**
 * UI language for the enrollment flow.
 * - `'en'` — English (default)
 * - `'ar'` — Arabic (enables RTL layout)
 */
export type EnrollLocalization = 'en' | 'ar';

/**
 * Forces the document scanning step to accept only a specific document type.
 * - `'nationalIdOnly'`        — only national ID
 * - `'passportOnly'`          — only passport
 * - `'nationalIdOrPassport'`  — user chooses (default)
 */
export type EnrollForcedDocumentType =
  | 'nationalIdOnly'
  | 'passportOnly'
  | 'nationalIdOrPassport';

/**
 * Individual enrollment step identifiers.
 * Used with `enrollExitStep` to terminate the flow after a specific step.
 */
export type EnrollStepType =
  | 'phoneOtp'
  | 'personalConfirmation'
  | 'smileLiveness'
  | 'emailOtp'
  | 'saveMobileDevice'
  | 'deviceLocation'
  | 'password'
  | 'securityQuestions'
  | 'amlCheck'
  | 'termsAndConditions'
  | 'electronicSignature'
  | 'ntraCheck'
  | 'csoCheck';

// ---------------------------------------------------------------------------
// Color types
// ---------------------------------------------------------------------------

/**
 * An RGBA color value.
 * `r`, `g`, `b` are integers 0–255. `opacity` is a float 0.0–1.0 (defaults to 1.0).
 */
export interface EnrollColor {
  r: number;
  g: number;
  b: number;
  opacity?: number;
}

/**
 * Custom color overrides for the enrollment UI.
 * Every property is optional — omitted colors fall back to the SDK defaults.
 */
export interface EnrollColors {
  primary?: EnrollColor;
  secondary?: EnrollColor;
  appBackgroundColor?: EnrollColor;
  textColor?: EnrollColor;
  errorColor?: EnrollColor;
  successColor?: EnrollColor;
  warningColor?: EnrollColor;
  appWhite?: EnrollColor;
  appBlack?: EnrollColor;
}

// ---------------------------------------------------------------------------
// Options
// ---------------------------------------------------------------------------

/**
 * Configuration object passed to {@link EnrollPlugin.startEnroll}.
 */
export interface StartEnrollOptions {
  // ---- Required ----

  /** Organization tenant ID. */
  tenantId: string;

  /** Organization tenant secret. */
  tenantSecret: string;

  /** SDK flow mode. */
  enrollMode: EnrollMode;

  // ---- Conditionally required ----

  /** Applicant / application ID. Required for `auth` and `update` modes. */
  applicantId?: string;

  /** Level-of-trust token. Required for `auth` mode. */
  levelOfTrust?: string;

  /** Contract template ID. Required for `signContract` mode. */
  templateId?: string;

  // ---- Optional ----

  /** Target environment. Defaults to `'staging'`. */
  enrollEnvironment?: EnrollEnvironment;

  /** UI language. Defaults to `'en'`. */
  localizationCode?: EnrollLocalization;

  /** Google Maps API key (used for location step). */
  googleApiKey?: string;

  /** Skip the tutorial screen. Defaults to `false`. */
  skipTutorial?: boolean;

  /** Correlation ID to link your user ID with the eNROLL request ID. */
  correlationId?: string;

  /** Resume a previous enrollment request by its ID. */
  requestId?: string;

  /** Extra contract parameters (JSON string) for `signContract` mode. */
  contractParameters?: string;

  /** Custom color overrides. */
  enrollColors?: EnrollColors;

  /** Force a specific document type for scanning. */
  enrollForcedDocumentType?: EnrollForcedDocumentType;

  /** Auto-close the SDK after this step completes successfully. */
  enrollExitStep?: EnrollStepType;
}

// ---------------------------------------------------------------------------
// Result types
// ---------------------------------------------------------------------------

/**
 * Returned when the enrollment flow completes successfully.
 */
export interface EnrollSuccessResult {
  /** The applicant ID assigned by the eNROLL backend. */
  applicantId: string;

  /** Human-readable success message from the SDK. */
  enrollMessage?: string;

  /** Document ID (if applicable). */
  documentId?: string;

  /** Request ID that can be used to resume the flow later. */
  requestId?: string;

  /** `true` if the flow ended early because `enrollExitStep` was reached. */
  exitStepCompleted: boolean;

  /** Name of the step that was completed when the flow exited early. */
  completedStepName?: string;
}

/**
 * Shape of the error returned when the enrollment flow fails.
 * Accessible via the rejected promise's `data` property.
 */
export interface EnrollErrorResult {
  /** Human-readable error message. */
  message: string;

  /** Machine-readable error code (if available). */
  code?: string;

  /** Applicant ID (if one was assigned before the error occurred). */
  applicantId?: string;
}

/**
 * Payload delivered by the `'onRequestId'` event listener.
 */
export interface EnrollRequestIdResult {
  /** The request ID generated by the SDK during the flow. */
  requestId: string;
}

// ---------------------------------------------------------------------------
// Plugin interface
// ---------------------------------------------------------------------------

/**
 * Capacitor plugin for the eNROLL Lite SDK.
 *
 * Provides eKYC identity verification for Ionic mobile apps on Android and iOS.
 * **Web / browser usage is not supported** — this plugin is for native mobile only.
 */
export interface EnrollPlugin {
  /**
   * Launch the eNROLL enrollment flow.
   *
   * Resolves with {@link EnrollSuccessResult} on success.
   * Rejects with an error whose `data` matches {@link EnrollErrorResult} on failure.
   *
   * @param options — configuration for the enrollment session
   */
  startEnroll(options: StartEnrollOptions): Promise<EnrollSuccessResult>;

  /**
   * Listen for the `'onRequestId'` event, which fires when the SDK
   * generates a request ID *during* the enrollment flow (before it completes).
   */
  addListener(
    eventName: 'onRequestId',
    listenerFunc: (result: EnrollRequestIdResult) => void,
  ): Promise<PluginListenerHandle>;

  /**
   * Remove all listeners registered by this plugin.
   */
  removeAllListeners(): Promise<void>;
}
