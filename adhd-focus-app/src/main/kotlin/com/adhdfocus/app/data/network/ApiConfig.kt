package com.adhdfocus.app.data.network

/**
 * Configuration matching calendar-mobile's AuthConfig.
 * Uses AWS Cognito for authentication and the real API Gateway endpoint.
 */
object ApiConfig {
    // Real API base URL - matches calendar-mobile AuthConfig.apiBaseUrl
    const val BASE_URL = "https://wd1nqv68vg.execute-api.us-east-1.amazonaws.com/prod/v1/"

    // AWS Cognito configuration - matches calendar-mobile AuthConfig
    object Cognito {
        const val CLIENT_ID = "42n973pvu7ev52rt467v0srsnf"
        const val ISSUER = "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_keKeXz5tJ"
        const val DISCOVERY_URL = "$ISSUER/.well-known/openid-configuration"
        const val REDIRECT_URI = "kinspacedev:/oauth2redirect"
        val SCOPES = listOf("openid", "email")
    }

    // Task management endpoints
    object Tasks {
        const val GET_TASKS = "households/{householdId}/todos"
        const val CREATE_TASK = "households/{householdId}/todos"
        const val UPDATE_TASK = "households/{householdId}/todos/{taskId}"
        const val DELETE_TASK = "households/{householdId}/todos/{taskId}"
    }

    // Sync endpoints
    object Sync {
        const val BATCH_SYNC = "households/{householdId}/sync"
        const val SYNC_STATUS = "households/{householdId}/sync/status"
    }

    // WebSocket endpoint
    object WebSocket {
        const val BASE_URL = ""
    }

    // Token storage keys - matches calendar-mobile key names
    object Token {
        const val HEADER_NAME = "Authorization"
        const val BEARER_PREFIX = "Bearer "
        const val ACCESS_TOKEN_KEY = "auth_access_token"
        const val ID_TOKEN_KEY = "auth_id_token"
        const val REFRESH_TOKEN_KEY = "auth_refresh_token"
        const val EXPIRY_KEY = "auth_access_token_expiry"
    }
}
