package com.b.a2.utils

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Gson 扩展函数集合
 * 提供更简洁的 JSON 序列化和反序列化操作
 */

// ==================== 基本扩展函数 ====================


/**
 * 将对象转换为 JSON 字符串
 * @param gson 可选的 Gson 实例，默认使用基础配置
 * @return JSON 字符串
 */
inline fun <reified T> T.toJson(gson: Gson = Gson()): String {
    return try {
        gson.toJson(this)
    } catch (e: Exception) {
        ""
    }
}

/**
 * 将对象转换为 JSON 字符串（安全抛出异常版本）
 * @param gson 可选的 Gson 实例，默认使用基础配置
 * @return JSON 字符串
 */
inline fun <reified T> T.toJsonOrThrow(gson: Gson = Gson()): String {
    return gson.toJson(this)
}

/**
 * 将 JSON 字符串转换为对象
 * @param gson 可选的 Gson 实例，默认使用基础配置
 * @return 解析后的对象，解析失败返回 null
 */
inline fun <reified T> String.fromJson(gson: Gson = Gson()): T? {
    return try {
        gson.fromJson(this, T::class.java)
    } catch (e: Exception) {
        null
    }
}

/**
 * 将 JSON 字符串转换为对象（安全抛出异常版本）
 * @param gson 可选的 Gson 实例，默认使用基础配置
 * @return 解析后的对象
 * @throws JsonSyntaxException 如果 JSON 格式不正确
 */
inline fun <reified T> String.fromJsonOrThrow(gson: Gson = Gson()): T {
    return gson.fromJson(this, T::class.java)
}

// ==================== 集合扩展函数 ====================

/**
 * 将集合转换为 JSON 数组字符串
 * @param gson 可选的 Gson 实例，默认使用基础配置
 * @return JSON 数组字符串
 */
fun <T> Collection<T>.toJsonArray(gson: Gson = Gson()): String {
    return gson.toJson(this)
}

/**
 * 将 JSON 数组字符串转换为集合
 * @param gson 可选的 Gson 实例，默认使用基础配置
 * @return 解析后的集合，解析失败返回空集合
 */
inline fun <reified T> String.fromJsonArray(gson: Gson = Gson()): List<T> {
    return try {
        gson.fromJson(this, object : TypeToken<List<T>>() {}.type)
    } catch (e: Exception) {
        emptyList()
    }
}

// ==================== 类型安全扩展函数 ====================

/**
 * 使用 Gson 实例将 JSON 字符串转换为指定类型对象
 * @param json JSON 字符串
 * @return 解析后的对象
 */
inline fun <reified T> Gson.fromJson(json: String): T {
    return this.fromJson(json, object : TypeToken<T>() {}.type)
}

/**
 * 使用 Gson 实例将对象转换为 JSON 字符串
 * @param obj 要转换的对象
 * @return JSON 字符串
 */
inline fun <reified T> Gson.toJson(obj: T): String {
    return this.toJson(obj, object : TypeToken<T>() {}.type)
}

// ==================== 文件操作扩展 ====================

/**
 * 从文件读取 JSON 并转换为对象
 * @param gson 可选的 Gson 实例，默认使用基础配置
 * @return 解析后的对象，文件不存在或解析失败返回 null
 */
inline fun <reified T> File.readJson(gson: Gson = Gson()): T? {
    return if (exists()) {
        try {
            gson.fromJson(readText(), T::class.java)
        } catch (e: Exception) {
            null
        }
    } else {
        null
    }
}

/**
 * 将对象写入文件为 JSON 格式
 * @param obj 要写入的对象
 * @param gson 可选的 Gson 实例，默认使用基础配置
 */
fun <T> File.writeJson(obj: T, gson: Gson = Gson()) {
    parentFile?.mkdirs()
    writeText(gson.toJson(obj))
}

// ==================== 特殊场景扩展 ====================

/**
 * 将 JSON 字符串转换为对象，带有默认值
 * @param defaultValue 解析失败时返回的默认值
 * @param gson 可选的 Gson 实例，默认使用基础配置
 * @return 解析后的对象或默认值
 */
inline fun <reified T> String.fromJsonOrDefault(
    defaultValue: T,
    gson: Gson = Gson()
): T {
    return try {
        gson.fromJson(this, T::class.java) ?: defaultValue
    } catch (e: Exception) {
        defaultValue
    }
}

/**
 * 将 JSON 字符串转换为对象，带有错误日志
 * @param tag 日志标签
 * @param gson 可选的 Gson 实例，默认使用基础配置
 * @return 解析后的对象，解析失败返回 null 并记录错误日志
 */
inline fun <reified T> String.fromJsonWithLog(
    tag: String = "GsonExt",
    gson: Gson = Gson()
): T? {
    return try {
        gson.fromJson(this, T::class.java)
    } catch (e: Exception) {
        Log.e(tag, "Failed to parse JSON: $this", e)
        null
    }
}

// ==================== 构建器扩展 ====================

/**
 * 创建带有默认配置的 Gson 实例
 * 包含常用配置：
 * - 支持序列化 null 值
 * - 格式化输出
 * - 支持复杂对象
 */
fun createDefaultGson(): Gson {
    return GsonBuilder()
        .serializeNulls()
        .setPrettyPrinting()
        .enableComplexMapKeySerialization()
        .create()
}

// ==================== 使用示例 ====================

/*
data class User(val name: String, val age: Int)

fun main() {
    // 创建带格式化的 Gson 实例
    val gson = createDefaultGson()
    
    // 对象转JSON
    val user = User("Alice", 30)
    val json = user.toJson(gson)
    println("对象转JSON:\n$json")
    
    // JSON转对象
    val parsedUser = json.fromJson<User>()
    println("\nJSON转对象:\n$parsedUser")
    
    // 集合操作
    val users = listOf(User("Bob", 25), User("Charlie", 35))
    val jsonArray = users.toJsonArray(gson)
    println("\n集合转JSON数组:\n$jsonArray")
    
    val parsedUsers = jsonArray.fromJsonArray<User>()
    println("\nJSON数组转集合:\n$parsedUsers")
    
    // 文件操作
    val file = File("user.json")
    file.writeJson(user, gson)
    println("\n对象写入文件: ${file.absolutePath}")
    
    val fileUser = file.readJson<User>()
    println("\n从文件读取对象:\n$fileUser")
    
    // 带默认值的解析
    val invalidJson = "{invalid}"
    val defaultUser = invalidJson.fromJsonOrDefault(User("Default", 0))
    println("\n带默认值的解析:\n$defaultUser")
}
*/