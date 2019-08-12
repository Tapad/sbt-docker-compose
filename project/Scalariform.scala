import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._

object Scalariform {
  lazy val settings = scalariformSettings ++
    (ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(PreserveSpaceBeforeArguments, true)
      .setPreference(AlignParameters, true)
      .setPreference(AlignArguments, true)
      .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)
      .setPreference(SpacesAroundMultiImports, false))
}
