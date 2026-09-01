package com.example.travelapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// ── Primari ──────────────────────────────────────────────────────────────────
val PrimaryBlue = Color(0xFF1B6FA8)
val PrimaryDark = Color(0xFF0D4A73)
val PrimaryLight = Color(0xFF4C9FD8)
val NavSelectedBlue = PrimaryBlue
val NavUnselected = Color(0xFF9CA3AF)

// ── Accenti ──────────────────────────────────────────────────────────────────
val AccentOrange = Color(0xFFF2994A)
val AccentOrangeDark = Color(0xFFF2A65A)

val TravelBlue = Color(0xFF0D5C96)
val TravelBlueDark = Color(0xFF083C63)
val TravelOrange = Color(0xFFE87A30)
val TravelOrangeHover = Color(0xFFCF661E)

// ── Palette Travel ────────────────────────────────────────────────────────────
// Alias equivalenti a quelli di "Sfondi, superfici e testi" più sotto, con nomi diversi.
val TravelBg: Color @Composable get() = MaterialTheme.colorScheme.background
val TravelSurface: Color @Composable get() = MaterialTheme.colorScheme.surface
val TravelTextDark: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val TravelTextMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val TravelBorder: Color @Composable get() = MaterialTheme.colorScheme.outline
val TravelChipBg: Color
    @Composable get() =
        if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
            Color(0xFF183247)
        } else {
            Color(0xFFEBF3F9)
        }

// ── Semantici ────────────────────────────────────────────────────────────────
val SuccessGreen = Color(0xFF2E9E5B)

val CompletedBadgeBackground: Color
    @Composable get() =
        if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
            Color(0xFF173A2A)
        } else {
            Color(0xFFE3F4E9)
        }

val CompletedBadgeText: Color
    @Composable get() =
        if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
            Color(0xFF7DDAA1)
        } else {
            Color(0xFF237A47)
        }
val WarningYellow = Color(0xFFB8860B)
val WarningBackground = Color(0xFFFFF4D6)
val ErrorRed = Color(0xFFEB5757)

val FavoriteRed = Color(0xFFE53935)
val LogoutRed = ErrorRed
val LogoutBackground = Color(0xFFFFEAEA)

// ── Sfondi, superfici e testi ────────────────────────────────────────────────
// Seguono MaterialTheme.colorScheme (i valori grezzi sono in Theme.kt) invece di
// restare fissi sulla versione chiara.
val BackgroundLight: Color @Composable get() = MaterialTheme.colorScheme.background
val BackgroundLavender: Color @Composable get() = MaterialTheme.colorScheme.background
val SurfaceLight: Color @Composable get() = MaterialTheme.colorScheme.surface
val SurfaceWhite: Color @Composable get() = MaterialTheme.colorScheme.surface

val TextPrimary: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val TextSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

// ── Utility ──────────────────────────────────────────────────────────────────
val DividerColor: Color @Composable get() = MaterialTheme.colorScheme.outline

/**
 * Bordo dei campi di testo. [DividerColor] va bene per separare due righe, ma su una
 * superficie chiara e' talmente tenue che il rettangolo del campo sparisce: qui serve
 * un grigio con contrasto pieno, sia sul tema chiaro sia su quello scuro.
 */
val FieldBorder: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val ChevronGrey: Color @Composable get() = MaterialTheme.colorScheme.outline
val SkeletonColor = Color(0xFFE9EAEC)

// ── Badge circolari ──────────────────────────────────────────────────────────
val BadgeBlue: Color
    @Composable get() =
        if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
            Color(0xFF183247)
        } else {
            Color(0xFFD0E8F5)
        }
val IconBlue = PrimaryBlue

val BadgeTeal: Color
    @Composable get() =
        if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
            Color(0xFF163B36)
        } else {
            Color(0xFFDDF5F0)
        }
val IconTeal = Color(0xFF13A594)

val BadgePink = Color(0xFFFFE4EC)
val IconPink = Color(0xFFE5397E)

val BadgePurple: Color
    @Composable get() =
        if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
            Color(0xFF2C2540)
        } else {
            Color(0xFFEDE4FF)
        }

val IconPurple = Color(0xFF7B4DFF)

val BadgeIndigo: Color
    @Composable get() =
        if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
            Color(0xFF252942)
        } else {
            Color(0xFFE6E9FF)
        }
val IconIndigo = Color(0xFF5B6BE1)

val BadgeGrey: Color
    @Composable get() =
        if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
            Color(0xFF2A2D32)
        } else {
            Color(0xFFECECF2)
        }
val IconGrey = Color(0xFF6B6A76)

// ── Card viaggio / Preferiti ─────────────────────────────────────────────────
val OutlineGrey = Color(0xFFE2E0EC)

val CoverPlaceholderStart = Color(0xFFD9E4FF)
val CoverPlaceholderEnd = Color(0xFFEFE3FF)
val CoverPlaceholderIcon = Color(0xFFB4B1C8)