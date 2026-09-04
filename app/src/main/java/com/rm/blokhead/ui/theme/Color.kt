package com.rm.blokhead.ui.theme

import androidx.compose.ui.graphics.Color

// Brand: cyan/amber against near-black chrome, so the GL well/HUD reads clearly and the
// UI never competes with the piece colors rendered inside the well itself.
val BrandCyan80 = Color(0xFFA6EFFF)
val BrandCyan40 = Color(0xFF0097A8)
val BrandAmber80 = Color(0xFFFFD9A0)
val BrandAmber40 = Color(0xFFB8720E)
val BrandMagenta80 = Color(0xFFFFB4E0)
val BrandMagenta40 = Color(0xFFA8296E)

val BackgroundLight = Color(0xFFF7FAFA)
val SurfaceLight = Color(0xFFEFF4F4)
val OnSurfaceLight = Color(0xFF161C1D)

val BackgroundDark = Color(0xFF0B0F10)
val SurfaceDark = Color(0xFF151B1D)
val OnSurfaceDark = Color(0xFFE3EDEE)

// Full brand-derived Material3 role set, defined explicitly for both themes so no role
// silently falls back to Material's unbranded baseline palette.
val OnPrimaryLight = Color.White
val PrimaryContainerLight = Color(0xFFCFF3F8)
val OnPrimaryContainerLight = Color(0xFF004E58)
val OnSecondaryLight = Color.White
val SecondaryContainerLight = Color(0xFFFFE4BE)
val OnSecondaryContainerLight = Color(0xFF7A4A00)
val OnTertiaryLight = Color.White
val TertiaryContainerLight = Color(0xFFFFDCEF)
val OnTertiaryContainerLight = Color(0xFF6E1148)
val SurfaceVariantLight = Color(0xFFDFE9E9)
val OnSurfaceVariantLight = Color(0xFF3F4949)
val OutlineLight = Color(0xFF6F7979)
val OutlineVariantLight = Color(0xFFBFC9C9)
val SurfaceContainerHighLight = Color(0xFFE5EFEF)

val OnPrimaryDark = Color(0xFF00363D)
val PrimaryContainerDark = Color(0xFF00505A)
val OnPrimaryContainerDark = BrandCyan80
val OnSecondaryDark = Color(0xFF422C00)
val SecondaryContainerDark = Color(0xFF5F4000)
val OnSecondaryContainerDark = BrandAmber80
val OnTertiaryDark = Color(0xFF450829)
val TertiaryContainerDark = Color(0xFF5F1339)
val OnTertiaryContainerDark = BrandMagenta80
val SurfaceVariantDark = Color(0xFF3F4949)
val OnSurfaceVariantDark = Color(0xFFC1CBCB)
val OutlineDark = Color(0xFF899393)
val OutlineVariantDark = Color(0xFF3F4949)
val SurfaceContainerHighDark = Color(0xFF20282A)
