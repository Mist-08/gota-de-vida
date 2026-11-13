package mx.gotadevida.core.db

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import mx.gotadevida.core.dominio.PuntoGeo
import mx.gotadevida.core.dominio.TipoSangre
import mx.gotadevida.donador.entity.Donador
import mx.gotadevida.reglas.entity.ReglasElegibilidad
import org.bson.BsonDocument
import org.bson.BsonDocumentReader
import org.bson.BsonDocumentWriter
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.kotlinx.KotlinSerializerCodec

/**
 * Verifica cómo se guardan las entidades en MongoDB sin necesitar un servidor:
 * el `_id`, el punto GeoJSON para el índice 2dsphere y los campos opcionales.
 */
class MapeoBsonTest {

    private inline fun <reified T : Any> ida(valor: T): BsonDocument {
        val codec = KotlinSerializerCodec.create<T>()!!
        val documento = BsonDocument()
        codec.encode(BsonDocumentWriter(documento), valor, EncoderContext.builder().build())
        return documento
    }

    private inline fun <reified T : Any> vuelta(documento: BsonDocument): T =
        KotlinSerializerCodec.create<T>()!!.decode(BsonDocumentReader(documento), DecoderContext.builder().build())

    @Test
    fun `un donador nuevo se guarda con _id y sin campos nulos, y se lee igual`() {
        val nuevo = Donador.nuevo("abc", "Mariana", null, "2026-11-12T00:00:00Z")

        val documento = ida(nuevo)

        assertEquals("abc", documento.getString("_id").value)
        assertFalse(documento.containsKey("tipoSangre"))
        assertEquals(true, documento.getBoolean("notificacionesActivas").value)
        assertEquals(nuevo, vuelta<Donador>(documento))
    }

    @Test
    fun `la ubicacion se guarda como punto GeoJSON con longitud primero`() {
        val donador = Donador.nuevo("abc", "Mariana", "8711234567", "2026-11-12T00:00:00Z").copy(
            tipoSangre = TipoSangre.O_NEGATIVO,
            ubicacion = PuntoGeo.de(latitud = 25.541, longitud = -103.407)
        )

        val ubicacion = ida(donador).getDocument("ubicacion")

        assertEquals("Point", ubicacion.getString("type").value)
        assertEquals(-103.407, ubicacion.getArray("coordinates")[0].asDouble().value)
        assertEquals(25.541, ubicacion.getArray("coordinates")[1].asDouble().value)
        assertEquals("O_NEGATIVO", ida(donador).getString("tipoSangre").value)
    }

    @Test
    fun `un documento sin los campos opcionales se puede leer`() {
        val documento = BsonDocument.parse(
            """{"_id": "x", "nombre": "Luis", "notificacionesActivas": true, "radioMaximoKm": 15,
               "fechasDonacion": [], "actualizadoEn": "2026-11-12T00:00:00Z"}"""
        )

        val donador = vuelta<Donador>(documento)

        assertNull(donador.ubicacion)
        assertNull(donador.telefono)
    }

    @Test
    fun `las reglas se guardan con el id vigentes`() {
        val documento = ida(ReglasElegibilidad.porDefecto("2026-11-12T00:00:00Z"))

        assertEquals(ReglasElegibilidad.ID_VIGENTES, documento.getString("_id").value)
        assertEquals(60, documento.getInt32("diasEntreDonaciones").value)
    }
}
