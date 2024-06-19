package ai.devchat.core

object DevChatActions {
    const val SEND_MESSAGE_REQUEST = "sendMessage/request"
    const val SEND_MESSAGE_RESPONSE = "sendMessage/response"
    const val REGENERATION_REQUEST = "regeneration/request"
    const val SEND_USER_MESSAGE_REQUEST = "sendUserMessage/request"
    const val SEND_USER_MESSAGE_RESPONSE = "sendUserMessage/response"
    const val CODE_DIFF_APPLY_REQUEST = "codeDiffApply/request"
    const val CODE_DIFF_APPLY_RESPONSE = "codeDiffApply/response"
    const val ADD_CONTEXT_NOTIFY = "addContext/notify"
    const val LIST_COMMANDS_REQUEST = "listCommands/request"
    const val LIST_COMMANDS_RESPONSE = "listCommands/response"
    const val LOAD_CONVERSATIONS_REQUEST = "loadConversations/request"
    const val LOAD_CONVERSATIONS_RESPONSE = "loadConversations/response"
    const val LOAD_HISTORY_MESSAGES_REQUEST = "loadHistoryMessages/request"
    const val LOAD_HISTORY_MESSAGES_RESPONSE = "loadHistoryMessages/response"
    const val OPEN_LINK_REQUEST = "openLink/request"
    const val OPEN_LINK_RESPONSE = "openLink/response"
    const val LIST_TOPICS_REQUEST = "listTopics/request"
    const val LIST_TOPICS_RESPONSE = "listTopics/response"
    const val INSERT_CODE_REQUEST = "insertCode/request"
    const val INSERT_CODE_RESPONSE = "insertCode/response"
    const val NEW_SRC_FILE_REQUEST = "newSrcFile/request"
    const val NEW_SRC_FILE_RESPONSE = "newSrcFile/response"
    const val REPLACE_FILE_CONTENT_REQUEST = "replaceFileContent/request"
    const val REPLACE_FILE_CONTENT_RESPONSE = "replaceFileContent/response"
    const val VIEW_DIFF_REQUEST = "viewDiff/request"
    const val VIEW_DIFF_RESPONSE = "viewDiff/response"
    const val GET_IDE_SERVICE_PORT_REQUEST = "getIDEServicePort/request"
    const val GET_IDE_SERVICE_PORT_RESPONSE = "getIDEServicePort/response"
    const val GET_SETTING_REQUEST = "getSetting/request"
    const val GET_SETTING_RESPONSE = "getSetting/response"
    const val UPDATE_SETTING_REQUEST = "updateSetting/request"
    const val UPDATE_SETTING_RESPONSE = "updateSetting/response"
    const val GET_SERVER_SETTINGS_REQUEST = "getServerSettings/request"
    const val GET_SERVER_SETTINGS_RESPONSE = "getServerSettings/response"
    const val UPDATE_SERVER_SETTINGS_REQUEST = "updateServerSettings/request"
    const val UPDATE_SERVER_SETTINGS_RESPONSE = "updateServerSettings/response"
    const val INPUT_REQUEST = "input/request"
    const val INPUT_RESPONSE = "input/response"
    const val STOP_GENERATION_REQUEST = "stopGeneration/request"
    const val STOP_GENERATION_RESPONSE = "stopGeneration/request"
    const val DELETE_LAST_CONVERSATION_REQUEST = "deleteLastConversation/request"
    const val DELETE_LAST_CONVERSATION_RESPONSE = "deleteLastConversation/response"
    const val DELETE_TOPIC_REQUEST = "deleteTopic/request"
    const val DELETE_TOPIC_RESPONSE = "deleteTopic/response"
}
