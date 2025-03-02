package com.extrabox.periodization.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

@Service
class FileStorageService {
    private val logger = LoggerFactory.getLogger(FileStorageService::class.java)

    @Value("\${file.storage.path:files}")
    private lateinit var storageDirectoryPath: String

    /**
     * Inicializa o diretório de armazenamento na inicialização do aplicativo
     */
    fun init() {
        try {
            val directory = File(storageDirectoryPath)
            if (!directory.exists()) {
                directory.mkdirs()
                logger.info("Diretório de armazenamento criado: {}", directory.absolutePath)
            }
        } catch (e: Exception) {
            logger.error("Não foi possível inicializar o diretório de armazenamento", e)
            throw RuntimeException("Não foi possível inicializar o diretório de armazenamento", e)
        }
    }

    /**
     * Salva o arquivo com o ID fornecido
     */
    fun saveFile(fileId: String, data: ByteArray): String {
        try {
            val filePath = Paths.get(storageDirectoryPath, "$fileId.xlsx")
            FileOutputStream(filePath.toFile()).use { outputStream ->
                outputStream.write(data)
            }
            logger.info("Arquivo salvo com sucesso: {}", filePath)
            return filePath.toString()
        } catch (e: IOException) {
            logger.error("Falha ao salvar o arquivo", e)
            throw RuntimeException("Falha ao salvar o arquivo", e)
        }
    }

    /**
     * Carrega o arquivo com o ID fornecido
     */
    fun loadFile(fileId: String): ByteArray {
        try {
            val filePath = Paths.get(storageDirectoryPath, "$fileId.xlsx")
            if (!Files.exists(filePath)) {
                throw RuntimeException("Arquivo não encontrado: $fileId.xlsx")
            }
            return Files.readAllBytes(filePath)
        } catch (e: IOException) {
            logger.error("Falha ao carregar o arquivo", e)
            throw RuntimeException("Falha ao carregar o arquivo", e)
        }
    }
}
