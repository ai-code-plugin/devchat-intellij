package ai.devchat.plugin.actions

import ai.devchat.common.DevChatBundle
import ai.devchat.core.DevChatActions
import ai.devchat.core.handlers.SendUserMessageHandler
import ai.devchat.plugin.DevChatToolWindow
import ai.devchat.storage.CONFIG
import com.alibaba.fastjson.JSONObject
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowManager

class DocComments : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.setEnabled(true)
        if ((CONFIG["language"] as? String) == "zh") {
            e.presentation.text = DevChatBundle.message("action.docComment.text.zh")
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (editor != null) {
            ToolWindowManager.getInstance(editor.project!!).getToolWindow("DevChat")?.show {
                val payload = JSONObject(mapOf("message" to "/comments"))
                if (DevChatToolWindow.loaded) {
                    SendUserMessageHandler(DevChatActions.SEND_USER_MESSAGE_REQUEST,null, payload).executeAction()
                } else {
                    SendUserMessageHandler.cache = payload
                }
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
