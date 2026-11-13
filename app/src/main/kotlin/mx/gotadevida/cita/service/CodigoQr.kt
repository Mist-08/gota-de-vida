package mx.gotadevida.cita.service

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.time.LocalDateTime
import java.time.ZoneId

/** Genera el QR de la cita en el teléfono (ZXing, sin internet). */
object CodigoQr {
    fun matriz(contenido: String, tamano: Int = 512): Array<BooleanArray> {
        val bits = QRCodeWriter().encode(
            contenido,
            BarcodeFormat.QR_CODE,
            tamano,
            tamano,
            mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 1)
        )
        return Array(bits.height) { y -> BooleanArray(bits.width) { x -> bits[x, y] } }
    }

    fun bitmap(contenido: String, tamano: Int = 512): Bitmap {
        val m = matriz(contenido, tamano)
        val pixeles = IntArray(tamano * tamano) { i -> if (m[i / tamano][i % tamano]) Color.BLACK else Color.WHITE }
        return Bitmap.createBitmap(pixeles, tamano, tamano, Bitmap.Config.ARGB_8888)
    }
}

/** Fechas de la cita en milisegundos para el calendario del teléfono (la donación dura cerca de 1 hora). */
object HorarioCita {
    private val zona: ZoneId = ZoneId.of("America/Monterrey")

    fun inicioMillis(fechaHora: String): Long = LocalDateTime.parse(fechaHora).atZone(zona).toInstant().toEpochMilli()

    fun finMillis(fechaHora: String): Long = inicioMillis(fechaHora) + 60 * 60 * 1000
}
