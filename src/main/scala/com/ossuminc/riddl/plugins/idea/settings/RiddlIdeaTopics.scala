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

class RiddlIdeaTopics {
  class UpdateToolWindow[T: ClassTag] {
    private class ToolWindowListenerTopic
        extends TopicWithID[T]("UpdateRiddlToolWindow") {}
  }
}
