package com.extrabox.periodization.config


import com.extrabox.periodization.service.FileStorageService
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FileStorageInitializer {

    @Bean
    fun initStorage(fileStorageService: FileStorageService): CommandLineRunner {
        return CommandLineRunner {
            fileStorageService.init()
        }
    }
}
