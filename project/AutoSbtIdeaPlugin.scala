import org.jetbrains.sbtidea.AbstractSbtIdeaPlugin

object AutoSbtIdeaPlugin extends AbstractSbtIdeaPlugin {
  override def trigger = allRequirements
}
