package com.firebaseui.android.demo.auth.fullcustomization.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

// The SVG's group opacity, applied per path: ImageVector groups have no alpha.
private const val OPACITY = 0.3f

private fun ImageVector.Builder.line(d: String) = addPath(
    pathData = addPathNodes(d),
    stroke = SolidColor(Color(0xFF00DCE2)),
    strokeAlpha = OPACITY,
    strokeLineWidth = 1.42395f,
)

private fun ImageVector.Builder.shape(d: String, color: Color) = addPath(
    pathData = addPathNodes(d),
    fill = SolidColor(color),
    fillAlpha = OPACITY,
)

/**
 * Grid's background: the iOS demo's grid-pattern.svg (Figma 20:5467 "Frame 5"), its gradient
 * fading to [page] so the same art works on the dark page. An [ImageVector] rather than a vector
 * drawable, which can't draw the gradient below API 24.
 */
fun gridPattern(page: Color): ImageVector =
    ImageVector.Builder(
        name = "GridPattern",
        defaultWidth = 997.dp,
        defaultHeight = 997.dp,
        viewportWidth = 997f,
        viewportHeight = 997f,
    ).apply {
        line("M209.246 6.92089L32.011 826.686")
        line("M987.671 220.381L167.906 43.1455")
        line("M587.813 88.7679L410.578 908.533")
        line("M905.824 598.948L86.059 421.712")
        line("M387.395 45.4372L210.16 865.202")
        line("M949.155 398.53L129.39 221.295")
        line("M765.962 127.284L588.727 947.049")
        line("M867.308 777.097L47.5433 599.862")
        line("M276.052 21.3645L98.8167 841.129")
        line("M973.228 287.186L153.463 109.951")
        line("M654.619 103.212L477.384 922.976")
        line("M891.381 665.754L71.6158 488.519")
        line("M454.202 59.8808L276.966 879.645")
        line("M934.712 465.336L114.947 288.101")
        line("M832.768 141.728L655.533 961.492")
        line("M852.865 843.903L33.1002 666.667")
        line("M342.857 35.8082L165.622 855.573")
        line("M958.784 353.992L139.02 176.757")
        line("M721.425 117.655L544.19 937.42")
        line("M876.937 732.559L57.1727 555.324")
        line("M521.007 74.3244L343.772 894.089")
        line("M920.268 532.142L100.503 354.907")
        line("M899.574 156.172L722.339 975.936")
        line("M838.421 910.709L18.6561 733.474")
        line("M231.514 11.7352L54.2792 831.5")
        line("M982.857 242.649L163.092 65.4141")
        line("M610.082 93.5827L432.847 913.347")
        line("M901.01 621.216L81.2452 443.981")
        line("M409.664 50.2519L232.429 870.017")
        line("M944.341 420.798L124.577 243.563")
        line("M788.231 132.099L610.996 951.863")
        line("M862.493 799.366L42.7286 622.131")
        line("M298.321 26.1788L121.086 845.943")
        line("M968.413 309.455L148.648 132.22")
        line("M676.888 108.026L499.653 927.791")
        line("M886.566 688.022L66.8011 510.787")
        line("M476.47 64.6952L299.235 884.46")
        line("M929.897 487.604L110.132 310.369")
        line("M855.037 146.542L677.802 966.307")
        line("M848.05 866.171L28.2855 688.936")
        line("M365.127 40.6226L187.892 860.387")
        line("M953.97 376.261L134.205 199.026")
        line("M743.694 122.47L566.458 942.234")
        line("M872.123 754.828L52.358 577.593")
        line("M543.275 79.1389L366.04 898.904")
        line("M915.454 554.41L95.6893 377.175")
        line("M921.842 160.986L744.607 980.751")
        line("M833.607 932.977L13.8423 755.742")
        line("M253.783 16.55L76.5484 836.315")
        line("M978.042 264.918L158.278 87.6827")
        line("M632.35 98.397L455.115 918.162")
        line("M896.195 643.485L76.4305 466.25")
        line("M431.932 55.0662L254.697 874.831")
        line("M939.526 443.067L119.761 265.832")
        line("M810.499 136.913L633.264 956.678")
        line("M857.679 821.634L37.9139 644.399")
        line("M320.589 30.9936L143.354 850.758")
        line("M963.599 331.724L143.834 154.489")
        line("M699.156 112.841L521.921 932.605")
        line("M881.752 710.291L61.9874 533.056")
        line("M498.738 69.5097L321.503 889.274")
        line("M925.082 509.873L105.318 332.638")
        line("M877.306 151.357L700.071 971.121")
        line("M843.235 888.44L23.4708 711.205")
        line("M387.395 45.4372L210.16 865.202")
        line("M949.155 398.53L129.39 221.295")
        line("M765.962 127.284L588.727 947.049")
        line("M867.308 777.097L47.5433 599.862")
        line("M565.545 83.9533L388.31 903.718")
        line("M910.639 576.679L90.8746 399.444")
        line("M944.112 165.8L766.877 985.565")
        line("M828.791 955.246L9.02666 778.011")
        group(rotate = 12.1997f, pivotX = 177.235f, pivotY = 0f) {
            addPath(
                pathData = addPathNodes("M177.235,0.0h838.705v838.705h-838.705z"),
                fill = Brush.linearGradient(
                    0.134118f to page, 0.5f to page.copy(alpha = 0f), 0.847192f to page,
                    start = Offset(597.3f, 0f),
                    end = Offset(597.153f, 838.705f),
                ),
                // Full alpha: under group opacity the SVG's mask hides the lines where it is
                // opaque, and a page-coloured fill on the page reproduces that.
                fillAlpha = 1f,
            )
        }
        shape("M563.338 135.969C562.988 130.004 569.059 125.771 574.535 128.162L636.67 155.29C642.406 157.795 643.179 165.616 638.045 169.196L579.885 209.749C574.751 213.329 567.679 209.899 567.312 203.651L563.338 135.969Z", Color(0xFFFF52B3))
        shape("M453.143 293.236C454.88 287.518 462.041 285.648 466.351 289.787L515.254 336.746C519.768 341.081 517.787 348.687 511.731 350.268L443.129 368.183C437.073 369.764 431.626 364.098 433.444 358.109L453.143 293.236Z", Color(0xFFFF52B3))
        shape("M455.3927,218.188a42.0363,42.0363 0,1 0,84.0726,0a42.0363,42.0363 0,1 0,-84.0726,0z", Color(0xFF00DCE2))
        shape("M548.4735000000001,311.769a51.5445,51.5445 0,1 0,103.089,0a51.5445,51.5445 0,1 0,-103.089,0z", Color(0xFFC6FF00))
        shape("M382.562 207.813C382.866 200.062 392.91 197.229 397.22 203.678L399.689 207.373C401.616 210.257 405.182 211.573 408.52 210.631L412.797 209.424C420.262 207.319 426.061 215.996 421.259 222.087L418.508 225.577C416.361 228.302 416.212 232.099 418.139 234.983L420.608 238.678C424.918 245.127 418.457 253.323 411.18 250.639L407.01 249.101C403.756 247.9 400.098 248.932 397.951 251.656L395.2 255.146C390.398 261.238 380.607 257.626 380.911 249.875L381.085 245.435C381.221 241.969 379.11 238.809 375.856 237.609L371.686 236.071C364.409 233.386 364.818 222.958 372.283 220.852L376.561 219.646C379.899 218.704 382.252 215.72 382.388 212.254L382.562 207.813Z", Color(0xFFC6FF00))
        shape("M671.453 195.059C677.539 190.251 686.222 196.041 684.124 203.508L682.922 207.786C681.984 211.126 683.302 214.69 686.189 216.614L689.886 219.079C696.339 223.382 693.516 233.43 685.766 233.742L681.326 233.92C677.86 234.06 674.878 236.415 673.939 239.755L672.737 244.033C670.639 251.501 660.211 251.92 657.52 244.646L655.977 240.478C654.774 237.225 651.612 235.117 648.146 235.256L643.705 235.435C635.955 235.747 632.334 225.959 638.42 221.151L641.908 218.397C644.63 216.247 645.658 212.588 644.454 209.335L642.912 205.167C640.22 197.893 648.409 191.424 654.863 195.727L658.56 198.192C661.446 200.116 665.243 199.964 667.965 197.813L671.453 195.059Z", Color(0xFF00DCE2))
    }.build()

