package com.adhdfocus.app.data.network

/**
 * Configuration for calendar-cloud API endpoints and base URLs
 */
object ApiConfig {
    // Base URL for calendar-cloud API
    const val BASE_URL = "https://api.calendar-cloud.example.com/"

    // Authentication endpoints
    object Auth {
        const val LOGIN = "api/auth/login"
        const val REFRESH = "api/auth/refresh"
        const val LOGOUT = "api/auth/logout"
    }

    // Task management endpoints
    object Tasks {
        const val GET_TASKS = "api/households/{householdId}/tasks"
        const val CREATE_TASK = "api/households/{householdId}/tasks"
        const val UPDATE_TASK = "api/households/{householdId}/tasks/{taskId}"
        const val DELETE_TASK = "api/households/{householdId}/tasks/{taskId}"
    }

    // Sync endpoints
    object Sync {
        const val BATCH_SYNC = "api/households/{householdId}/sync"
        const val SYNC_STATUS = "api/households/{householdId}/sync/status"
    }

    // WebSocket endpoints
    object WebSocket {
        const val BASE_URL = "wss://ws.calendar-cloud.example.com/"
        const val CONNECT = "ws/connect"
    }

    // Token configuration
    object Token {
        const val HEADER_NAME = "Authorization"
        const val BEARER_PREFIX = "Bearer "
        const val REFRESH_TOKEN_KEY = "refresh_token"
        const val ACCESS_TOKEN_KEY = "access_token"
    }
}
