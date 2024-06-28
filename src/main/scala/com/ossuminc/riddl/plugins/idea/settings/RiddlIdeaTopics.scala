package com.ossuminc.riddl.plugins.idea.settings

import com.intellij.util.messages.Topic

import scala.reflect.ClassTag

trait TopicWithID[T: ClassTag](topicId: String) {
  @Topic.ProjectLevel
  val id: String = topicId

  val TOPIC: Topic[T] = Topic.create(
    id,
    implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]
  )
}

object RiddlIdeaTopics {
  trait MessageListener[T: ClassTag](topicIdParam: String) {
    def settingsChanged(): Unit = {}

    abstract class ListenerTopic extends TopicWithID[T](topicIdParam) {}

    val listenerTopic: ListenerTopic

  }

  object UpdateToolWindow {
    class UpdateToolWindowListener
        extends MessageListener[UpdateToolWindowListener](topicIdParam =
          "UpdateRiddlToolWindow"
        ) {
      private class ToolWindowListenerTopic extends ListenerTopic {}

      override val listenerTopic: ListenerTopic = new ToolWindowListenerTopic()
    }
  }
}
