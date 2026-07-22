package com.linguatycoon.app

import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    fun evaluate(baseUrl: String, audio: File, native: String, target: String, prompt: String): JSONObject {
        val boundary = "LinguaBoundary${System.currentTimeMillis()}"
        val connection = URL("$baseUrl/api/lesson").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 20_000
        connection.readTimeout = 180_000
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        connection.outputStream.use { out ->
            fun field(name: String, value: String) {
                out.write("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n".toByteArray())
            }
            field("native_language", native)
            field("target_language", target)
            field("prompt", prompt)
            out.write("--$boundary\r\nContent-Disposition: form-data; name=\"audio\"; filename=\"speech.m4a\"\r\nContent-Type: audio/mp4\r\n\r\n".toByteArray())
            audio.inputStream().use { it.copyTo(out) }
            out.write("\r\n--$boundary--\r\n".toByteArray())
        }
        val body = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
            .bufferedReader().use { it.readText() }
        if (connection.responseCode !in 200..299) error("Server ${connection.responseCode}: $body")
        return JSONObject(body)
    }

    fun latestRelease(repository: String): JSONObject {
        val connection = URL("https://api.github.com/repos/$repository/releases/latest").openConnection() as HttpURLConnection
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "LinguaTycoon-Android")
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        val body = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
            .bufferedReader().use { it.readText() }
        if (connection.responseCode !in 200..299) error("GitHub ${connection.responseCode}: $body")
        return JSONObject(body)
    }
}
