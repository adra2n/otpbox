package com.otpbox.di

import android.content.Context
import com.otpbox.data.backup.BackupEncryptor
import com.otpbox.data.crypto.DbKeyManager
import com.otpbox.data.crypto.SecurePrefs
import com.otpbox.data.local.OtpDao
import com.otpbox.data.local.OtpDatabase
import com.otpbox.data.repo.OtpRepository
import com.otpbox.data.repo.OtpRepositoryImpl
import com.otpbox.data.settings.SettingsRepository
import com.otpbox.data.sync.GitHubApi
import com.otpbox.data.sync.SyncManager
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import androidx.room.Room
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideDbKeyManager(@ApplicationContext context: Context): DbKeyManager =
        DbKeyManager(context)

    @Provides
    @Singleton
    fun provideSecurePrefs(@ApplicationContext context: Context): SecurePrefs =
        SecurePrefs(context)

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepository(context)

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        dbKeyManager: DbKeyManager
    ): OtpDatabase {
        val passphrase = dbKeyManager.getOrCreatePassphrase()
        val factory = SupportFactory(passphrase)
        return Room.databaseBuilder(context, OtpDatabase::class.java, OtpDatabase.NAME)
            .openHelperFactory(factory)
            .addMigrations(OtpDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideOtpDao(database: OtpDatabase): OtpDao = database.otpDao()

    @Provides
    @Singleton
    fun provideOtpRepository(dao: OtpDao): OtpRepository = OtpRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideBackupEncryptor(json: Json): BackupEncryptor = BackupEncryptor(json)

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideGitHubApi(client: OkHttpClient, json: Json): GitHubApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(GitHubApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(GitHubApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSyncManager(
        api: GitHubApi,
        repository: OtpRepository,
        encryptor: BackupEncryptor,
        securePrefs: SecurePrefs
    ): SyncManager = SyncManager(api, repository, encryptor, securePrefs)
}
