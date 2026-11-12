package mx.gotadevida.core.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import mx.gotadevida.BuildConfig
import mx.gotadevida.autenticacion.repository.AutenticacionApi
import mx.gotadevida.autenticacion.repository.AutenticacionRepository
import mx.gotadevida.autenticacion.repository.AutenticacionRepositoryRed
import mx.gotadevida.core.database.GotaDatabase
import mx.gotadevida.core.network.InterceptorToken
import mx.gotadevida.core.network.RenovadorSesion
import mx.gotadevida.core.sesion.AlmacenSesion
import mx.gotadevida.core.sesion.AlmacenSesionDataStore
import mx.gotadevida.donador.repository.DonadorApi
import mx.gotadevida.donador.repository.DonadorDao
import mx.gotadevida.donador.repository.DonadorRepository
import mx.gotadevida.donador.repository.DonadorRepositoryLocalYRed
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/** Red: un solo Retrofit para toda la app, con token y renovación automática. */
@Module
@InstallIn(SingletonComponent::class)
object ModuloRed {

    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun clienteHttp(almacen: AlmacenSesion, json: Json): OkHttpClient {
        val registro = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        // Render gratis tarda cerca de un minuto en despertar: tiempos de espera generosos.
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(70, TimeUnit.SECONDS)
            .addInterceptor(InterceptorToken(almacen))
            .addInterceptor(registro)
            .authenticator(RenovadorSesion(almacen, BuildConfig.API_BASE_URL, json))
            .build()
    }

    @Provides
    @Singleton
    fun retrofit(cliente: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(cliente)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun autenticacionApi(retrofit: Retrofit): AutenticacionApi = retrofit.create(AutenticacionApi::class.java)

    @Provides
    @Singleton
    fun donadorApi(retrofit: Retrofit): DonadorApi = retrofit.create(DonadorApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object ModuloBaseDeDatos {

    @Provides
    @Singleton
    fun baseDeDatos(@ApplicationContext contexto: Context): GotaDatabase =
        Room.databaseBuilder(contexto, GotaDatabase::class.java, GotaDatabase.NOMBRE).build()

    @Provides
    fun donadorDao(baseDeDatos: GotaDatabase): DonadorDao = baseDeDatos.donadorDao()
}

/** Une cada interfaz con su implementación. Las pruebas usan versiones falsas de las mismas interfaces. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ModuloEnlaces {

    @Binds
    @Singleton
    abstract fun almacenSesion(implementacion: AlmacenSesionDataStore): AlmacenSesion

    @Binds
    abstract fun autenticacionRepository(implementacion: AutenticacionRepositoryRed): AutenticacionRepository

    @Binds
    abstract fun donadorRepository(implementacion: DonadorRepositoryLocalYRed): DonadorRepository
}
