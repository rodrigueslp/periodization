package com.extrabox.periodization.service

import com.extrabox.periodization.model.BikeAthleteData
import com.itextpdf.html2pdf.HtmlConverter
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class BikePdfGenerationService {
    private val logger = LoggerFactory.getLogger(BikePdfGenerationService::class.java)

    @Value("\${app.logo.url:http://localhost:3000/static/media/planilize-logo.854402b5f477121cfbd7.png}")
    private lateinit var logoUrl: String

    fun generatePdf(athleteData: BikeAthleteData, planContent: String): ByteArray {
        try {
            val html = buildHtmlFromMarkdown(athleteData, planContent)
            val outputStream = ByteArrayOutputStream()
            HtmlConverter.convertToPdf(ByteArrayInputStream(html.toByteArray()), outputStream)
            return outputStream.toByteArray()
        } catch (e: Exception) {
            logger.error("Erro ao gerar PDF do plano de bike", e)
            throw RuntimeException("Falha ao gerar PDF: ${e.message}", e)
        }
    }

    private fun buildHtmlFromMarkdown(athleteData: BikeAthleteData, planContent: String): String {
        val extensions = listOf(
            TablesExtension.create(),
            HeadingAnchorExtension.create()
        )

        val parser = Parser.builder()
            .extensions(extensions)
            .build()

        val renderer = HtmlRenderer.builder()
            .extensions(extensions)
            .build()

        val document = parser.parse(planContent)
        val contentHtml = renderer.render(document)

        return buildFullHtml(athleteData, contentHtml)
    }

    private fun buildFullHtml(athleteData: BikeAthleteData, contentHtml: String): String {
        val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Plano de Treino de Bike - ${athleteData.nome}</title>
                <style>
                    ${getCssStyles()}
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="logo-section">
                        <img src="$logoUrl" alt="Planilize Logo" class="logo" onerror="this.style.display='none'">
                        <div class="brand-info">
                            <h1>PLANILIZE</h1>
                            <p>Planos de Treino Personalizados</p>
                        </div>
                    </div>
                    <div class="plan-info">
                        <h2>PLANO DE TREINO DE BIKE</h2>
                        <p class="athlete-name">${athleteData.nome}</p>
                        <p class="generation-date">Gerado em: $currentDate</p>
                    </div>
                </div>

                <div class="athlete-summary">
                    <div class="summary-grid">
                        <div class="summary-item">
                            <span class="label">Atleta:</span>
                            <span class="value">${athleteData.nome}</span>
                        </div>
                        <div class="summary-item">
                            <span class="label">Idade:</span>
                            <span class="value">${athleteData.idade} anos</span>
                        </div>
                        <div class="summary-item">
                            <span class="label">Experiência:</span>
                            <span class="value">${athleteData.experiencia}</span>
                        </div>
                        <div class="summary-item">
                            <span class="label">Objetivo:</span>
                            <span class="value">${athleteData.objetivo}</span>
                        </div>
                        <div class="summary-item">
                            <span class="label">Dias/semana:</span>
                            <span class="value">${athleteData.diasDisponiveis}</span>
                        </div>
                        <div class="summary-item">
                            <span class="label">Volume atual:</span>
                            <span class="value">${athleteData.volumeSemanalAtual}h/sem</span>
                        </div>
                        ${if (!athleteData.tipoBike.isNullOrBlank()) {
                            """
                            <div class="summary-item">
                                <span class="label">Tipo de bike:</span>
                                <span class="value">${athleteData.tipoBike}</span>
                            </div>
                            """
                        } else ""}
                        ${if (athleteData.ftpAtual != null) {
                            """
                            <div class="summary-item">
                                <span class="label">FTP atual:</span>
                                <span class="value">${athleteData.ftpAtual}W</span>
                            </div>
                            """
                        } else ""}
                    </div>
                </div>

                <div class="content">
                    $contentHtml
                </div>

                <div class="footer">
                    <p>&copy; ${LocalDate.now().year} Planilize - Todos os direitos reservados</p>
                    <p>Este plano foi gerado especificamente para ${athleteData.nome} e não deve ser compartilhado.</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun getCssStyles(): String {
        return """
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }

            body {
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                line-height: 1.6;
                color: #333;
                background-color: #fff;
                padding: 20px;
            }

            .header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                padding: 20px 0;
                border-bottom: 3px solid #3B82F6;
                margin-bottom: 30px;
                background: linear-gradient(135deg, #F8FAFC 0%, #E2E8F0 100%);
                padding: 20px;
                border-radius: 8px;
            }

            .logo-section {
                display: flex;
                align-items: center;
                gap: 15px;
            }

            .logo {
                height: 60px;
                width: auto;
            }

            .brand-info h1 {
                color: #3B82F6;
                font-size: 28px;
                font-weight: bold;
                margin-bottom: 5px;
            }

            .brand-info p {
                color: #64748B;
                font-size: 14px;
                font-weight: 500;
            }

            .plan-info {
                text-align: right;
            }

            .plan-info h2 {
                color: #1E293B;
                font-size: 24px;
                margin-bottom: 10px;
                font-weight: 700;
            }

            .athlete-name {
                color: #3B82F6;
                font-size: 18px;
                font-weight: 600;
                margin-bottom: 5px;
            }

            .generation-date {
                color: #64748B;
                font-size: 12px;
            }

            .athlete-summary {
                background: #F1F5F9;
                padding: 20px;
                border-radius: 8px;
                margin-bottom: 30px;
                border-left: 5px solid #3B82F6;
            }

            .summary-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                gap: 15px;
            }

            .summary-item {
                display: flex;
                flex-direction: column;
                gap: 5px;
            }

            .summary-item .label {
                font-weight: 600;
                color: #475569;
                font-size: 12px;
                text-transform: uppercase;
                letter-spacing: 0.5px;
            }

            .summary-item .value {
                font-weight: 500;
                color: #1E293B;
                font-size: 14px;
            }

            .content {
                max-width: 100%;
                overflow-wrap: break-word;
            }

            h1 {
                color: #1E293B;
                font-size: 28px;
                margin: 30px 0 20px 0;
                padding-bottom: 10px;
                border-bottom: 2px solid #3B82F6;
                page-break-after: avoid;
            }

            h2 {
                color: #3B82F6;
                font-size: 22px;
                margin: 25px 0 15px 0;
                page-break-after: avoid;
            }

            h3 {
                color: #1E293B;
                font-size: 18px;
                margin: 20px 0 12px 0;
                page-break-after: avoid;
            }

            h4 {
                color: #475569;
                font-size: 16px;
                margin: 15px 0 10px 0;
                page-break-after: avoid;
            }

            p {
                margin-bottom: 12px;
                text-align: justify;
                line-height: 1.7;
            }

            ul, ol {
                margin: 15px 0;
                padding-left: 25px;
            }

            li {
                margin-bottom: 8px;
                line-height: 1.6;
            }

            strong {
                color: #1E293B;
                font-weight: 600;
            }

            em {
                color: #3B82F6;
                font-style: italic;
            }

            table {
                width: 100%;
                border-collapse: collapse;
                margin: 20px 0;
                background: #fff;
                box-shadow: 0 1px 3px rgba(0,0,0,0.1);
                border-radius: 8px;
                overflow: hidden;
            }

            th {
                background: #3B82F6;
                color: white;
                padding: 12px;
                text-align: left;
                font-weight: 600;
                font-size: 14px;
            }

            td {
                padding: 12px;
                border-bottom: 1px solid #E5E7EB;
                font-size: 14px;
            }

            tr:hover {
                background: #F8FAFC;
            }

            .footer {
                margin-top: 50px;
                padding-top: 20px;
                border-top: 2px solid #E5E7EB;
                text-align: center;
                color: #64748B;
                font-size: 12px;
            }

            .footer p {
                margin-bottom: 5px;
            }

            blockquote {
                border-left: 4px solid #3B82F6;
                padding-left: 20px;
                margin: 20px 0;
                background: #F8FAFC;
                padding: 15px 20px;
                border-radius: 0 8px 8px 0;
                font-style: italic;
                color: #475569;
            }

            code {
                background: #F1F5F9;
                padding: 2px 6px;
                border-radius: 4px;
                font-family: 'Courier New', monospace;
                color: #3B82F6;
                font-size: 0.9em;
            }

            pre {
                background: #1E293B;
                color: #F1F5F9;
                padding: 20px;
                border-radius: 8px;
                overflow-x: auto;
                margin: 20px 0;
                font-family: 'Courier New', monospace;
                font-size: 14px;
                line-height: 1.4;
            }

            @media print {
                body {
                    padding: 0;
                }
                
                .header {
                    break-inside: avoid;
                }
                
                h1, h2, h3, h4 {
                    page-break-after: avoid;
                }
                
                table {
                    page-break-inside: avoid;
                }
            }
        """.trimIndent()
    }
}
