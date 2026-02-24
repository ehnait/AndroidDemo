import groovy.json.JsonSlurper

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
}

if (getChannelConfigs().any { it.second["firebase"] == true }) {
    apply(plugin = "com.google.gms.google-services")
}

var _cachedChannelConfigs: List<Pair<String, Map<String, Any>>>? = null

fun getChannelConfigs(): List<Pair<String, Map<String, Any>>> {
    if (_cachedChannelConfigs == null) {
        val channelDir = File(rootProject.projectDir, "channel")
        _cachedChannelConfigs = channelDir.listFiles()?.filter { it.isDirectory }?.mapNotNull { channelFolder ->
            val configFile = File(channelFolder, "buildconfig.json")
            configFile.takeIf { it.exists() }?.let {
                channelFolder.name to JsonSlurper().parse(it) as Map<String, Any>
            }
        } ?: emptyList()
    }
    return _cachedChannelConfigs!!
}


android {
    namespace = "com.b.a2"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.b.a2"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getChannelConfigs().forEach { (folderName, config) ->
            create(folderName) {
                storeFile = File(rootProject.projectDir, "channel/$folderName/key.keystore")
                storePassword = "123456"
                keyAlias = config["keyAlias"] as String
                keyPassword = "123456"

                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    flavorDimensions += "country"
    productFlavors {
        getChannelConfigs().forEach { (folderName, config) ->
            create(folderName) {
                dimension = "country"
                applicationId = config["pkg"] as String
                versionCode = (config["versionCode"] as? Int) ?: 100
                versionName = (config["versionName"] as? String) ?: "1.0.0"
                signingConfig = signingConfigs.getByName(folderName)

                resValue("string", "app_name", config["appName"] as String)

                buildConfigField("String", "CHANNEL", "\"${folderName}\"")
                buildConfigField("String", "QUEST_CHANNEL", "\"${config["questChannel"]}\"")
                buildConfigField("String", "AREA", "\"${config["area"]}\"")
                buildConfigField("boolean", "SANDBOX", "${config["sandbox"] ?: false}")
                buildConfigField("String", "DEFAULT_URL", "\"${config["defaultUrl"]}\"")
                buildConfigField("String", "API_SERVER", "\"${config["apiServer"]}\"")
            }
        }
    }

    sourceSets {
        getChannelConfigs().forEach { (folderName, _) ->
            getByName(folderName) {
                setRoot("${rootProject.projectDir}/channel/${folderName}")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    applicationVariants.all {
        val variantName = name
        val flavorName = flavorName
        val config = getChannelConfigs().find { it.first == flavorName }?.second

        val googleTask = tasks.matching { it.name.contains("GoogleServices") && it.name.contains(variantName, ignoreCase = true) }

//        if (config?.get("firebase") != true) {
//            googleTask.configureEach {
//                enabled = false
//            }
//        } else {
//            // Copy google-services.json for Firebase-enabled flavors
//            googleTask.configureEach {
//                doFirst {
//                    val jsonFile = File(rootProject.projectDir, "channel/$flavorName/google-services.json")
//                    if (jsonFile.exists()) {
//                        println("Copying google-services.json from ${jsonFile.absolutePath} to app/")
//                        copy {
//                            from(jsonFile)
//                            into(projectDir)
//                        }
//                    } else {
//                        println("Warning: google-services.json not found at ${jsonFile.absolutePath}")
//                    }
//                }
//            }
//        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.hilt.android)
    implementation(libs.androidx.constraintlayout)
    kapt(libs.hilt.compiler)
    implementation(libs.coil)
    implementation(libs.coil.gif)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.adjust.android)
    implementation(libs.installreferrer)
    implementation(libs.androidx.browser)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

}
// Allow references to generated code
kapt {
    correctErrorTypes = true
}

