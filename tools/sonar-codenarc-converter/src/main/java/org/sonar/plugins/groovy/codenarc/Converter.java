/*
 * Sonar CodeNarc Converter
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.groovy.codenarc;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import org.codenarc.rule.AbstractRule;
import org.sonar.plugins.groovy.codenarc.apt.AptParser;
import org.sonar.plugins.groovy.codenarc.apt.AptResult;
import org.sonar.plugins.groovy.codenarc.printer.Printer;
import org.sonar.plugins.groovy.codenarc.printer.XMLPrinter;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class Converter {

  /**
   * location of the generated file
   */
  public static final File RESULTS_FOLDER = new File("target/results");

  /**
   * location of the apt files in the CodeNarc project
   */
  private static final String RULES_APT_FILES_LOCATION = "../../../CodeNarc/src/site/apt";

  private int count = 0;
  private Map<String, Integer> rulesByVersion = Maps.newHashMap();
  private Map<String, Integer> rulesByTags = Maps.newHashMap();
  private Set<String> duplications = new HashSet<>();

  public static void main(String[] args) throws Exception {
    Converter converter = new Converter();

    process(converter, new XMLPrinter());

    converter.resultsByCategory();
    converter.resultsByVersion();
    System.out.println();
    System.out.println(converter.count + " rules processed");
  }

  private static void process(Converter converter, Printer printer) throws Exception {
    checkResultFolder();
    printer.init(converter).process(Converter.loadRules()).printAll(RESULTS_FOLDER);
  }

  private static void checkResultFolder() {
    RESULTS_FOLDER.mkdirs();
  }

  public static Multimap<RuleSet, Rule> loadRules() throws Exception {
    Properties props = new Properties();
    props.load(Converter.class.getResourceAsStream("/codenarc-base-messages.properties"));

    Map<String, AptResult> parametersByRule = retrieveRulesParameters();

    Multimap<RuleSet, Rule> rules = LinkedListMultimap.create();

    insertRules(rules, /* legacy */null, props, parametersByRule,
      org.codenarc.rule.unused.UnusedArrayRule.class,
      org.codenarc.rule.unused.UnusedObjectRule.class,
      org.codenarc.rule.unused.UnusedPrivateFieldRule.class,
      org.codenarc.rule.unused.UnusedPrivateMethodRule.class,
      org.codenarc.rule.unused.UnusedVariableRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryBooleanExpressionRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryIfStatementRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryTernaryExpressionRule.class,
      org.codenarc.rule.size.ClassSizeRule.class,
      org.codenarc.rule.size.CyclomaticComplexityRule.class,
      org.codenarc.rule.size.MethodCountRule.class,
      org.codenarc.rule.size.MethodSizeRule.class,
      org.codenarc.rule.size.NestedBlockDepthRule.class,
      // org.codenarc.rule.size.AbcComplexityRule.class, - deprecated in 0.18
      org.codenarc.rule.naming.AbstractClassNameRule.class,
      org.codenarc.rule.naming.ClassNameRule.class,
      org.codenarc.rule.naming.FieldNameRule.class,
      org.codenarc.rule.naming.InterfaceNameRule.class,
      org.codenarc.rule.naming.MethodNameRule.class,
      org.codenarc.rule.naming.PackageNameRule.class,
      org.codenarc.rule.naming.ParameterNameRule.class,
      org.codenarc.rule.naming.PropertyNameRule.class,
      org.codenarc.rule.naming.VariableNameRule.class,
      org.codenarc.rule.logging.PrintlnRule.class,
      org.codenarc.rule.logging.PrintStackTraceRule.class,
      org.codenarc.rule.logging.SystemErrPrintRule.class,
      org.codenarc.rule.logging.SystemOutPrintRule.class,
      org.codenarc.rule.junit.JUnitAssertAlwaysFailsRule.class,
      org.codenarc.rule.junit.JUnitAssertAlwaysSucceedsRule.class,
      org.codenarc.rule.junit.JUnitPublicNonTestMethodRule.class,
      org.codenarc.rule.junit.JUnitSetUpCallsSuperRule.class,
      org.codenarc.rule.junit.JUnitTearDownCallsSuperRule.class,
      org.codenarc.rule.junit.JUnitUnnecessarySetUpRule.class,
      org.codenarc.rule.junit.JUnitUnnecessaryTearDownRule.class,
      org.codenarc.rule.imports.DuplicateImportRule.class,
      org.codenarc.rule.imports.ImportFromSamePackageRule.class,
      org.codenarc.rule.imports.UnnecessaryGroovyImportRule.class,
      org.codenarc.rule.imports.UnusedImportRule.class,
      org.codenarc.rule.grails.GrailsPublicControllerMethodRule.class,
      org.codenarc.rule.grails.GrailsSessionReferenceRule.class,
      org.codenarc.rule.grails.GrailsServletContextReferenceRule.class,
      org.codenarc.rule.grails.GrailsStatelessServiceRule.class,
      org.codenarc.rule.generic.IllegalRegexRule.class,
      org.codenarc.rule.generic.RequiredRegexRule.class,
      org.codenarc.rule.generic.RequiredStringRule.class,
      org.codenarc.rule.generic.StatelessClassRule.class,
      org.codenarc.rule.exceptions.CatchErrorRule.class,
      org.codenarc.rule.exceptions.CatchExceptionRule.class,
      org.codenarc.rule.exceptions.CatchNullPointerExceptionRule.class,
      org.codenarc.rule.exceptions.CatchRuntimeExceptionRule.class,
      org.codenarc.rule.exceptions.CatchThrowableRule.class,
      org.codenarc.rule.exceptions.ThrowErrorRule.class,
      org.codenarc.rule.exceptions.ThrowExceptionRule.class,
      org.codenarc.rule.exceptions.ThrowNullPointerExceptionRule.class,
      org.codenarc.rule.exceptions.ThrowRuntimeExceptionRule.class,
      org.codenarc.rule.exceptions.ThrowThrowableRule.class,
      org.codenarc.rule.basic.BigDecimalInstantiationRule.class,
      org.codenarc.rule.basic.ConstantIfExpressionRule.class,
      org.codenarc.rule.basic.ConstantTernaryExpressionRule.class,
      org.codenarc.rule.basic.EmptyCatchBlockRule.class,
      org.codenarc.rule.basic.EmptyElseBlockRule.class,
      org.codenarc.rule.basic.EmptyFinallyBlockRule.class,
      org.codenarc.rule.basic.EmptyForStatementRule.class,
      org.codenarc.rule.basic.EmptyIfStatementRule.class,
      org.codenarc.rule.basic.EmptySwitchStatementRule.class,
      org.codenarc.rule.basic.EmptySynchronizedStatementRule.class,
      org.codenarc.rule.basic.EmptyTryBlockRule.class,
      org.codenarc.rule.basic.EmptyWhileStatementRule.class,
      org.codenarc.rule.basic.EqualsAndHashCodeRule.class,
      org.codenarc.rule.basic.ReturnFromFinallyBlockRule.class,
      org.codenarc.rule.basic.ThrowExceptionFromFinallyBlockRule.class,
      org.codenarc.rule.braces.IfStatementBracesRule.class,
      org.codenarc.rule.braces.ElseBlockBracesRule.class,
      org.codenarc.rule.braces.ForStatementBracesRule.class,
      org.codenarc.rule.braces.WhileStatementBracesRule.class,
      org.codenarc.rule.concurrency.NestedSynchronizationRule.class,
      org.codenarc.rule.concurrency.SynchronizedMethodRule.class,
      org.codenarc.rule.concurrency.SynchronizedOnThisRule.class,
      org.codenarc.rule.concurrency.SystemRunFinalizersOnExitRule.class,
      org.codenarc.rule.concurrency.ThreadGroupRule.class,
      org.codenarc.rule.concurrency.ThreadLocalNotStaticFinalRule.class,
      org.codenarc.rule.concurrency.ThreadYieldRule.class,
      org.codenarc.rule.concurrency.VolatileLongOrDoubleFieldRule.class,
      // moved from basic in 0.16
      org.codenarc.rule.design.CloneableWithoutCloneRule.class,
      org.codenarc.rule.design.ImplementationAsTypeRule.class);

    insertRules(rules, "0.11", props, parametersByRule,
      org.codenarc.rule.naming.ConfusingMethodNameRule.class,
      org.codenarc.rule.naming.ObjectOverrideMisspelledMethodNameRule.class,
      org.codenarc.rule.junit.JUnitStyleAssertionsRule.class,
      org.codenarc.rule.junit.UseAssertEqualsInsteadOfAssertTrueRule.class,
      org.codenarc.rule.junit.UseAssertFalseInsteadOfNegationRule.class,
      org.codenarc.rule.junit.UseAssertTrueInsteadOfAssertEqualsRule.class,
      org.codenarc.rule.junit.UseAssertNullInsteadOfAssertEqualsRule.class,
      org.codenarc.rule.junit.UseAssertSameInsteadOfAssertTrueRule.class,
      org.codenarc.rule.junit.JUnitFailWithoutMessageRule.class,
      org.codenarc.rule.exceptions.CatchIllegalMonitorStateExceptionRule.class,
      org.codenarc.rule.exceptions.ConfusingClassNamedExceptionRule.class,
      org.codenarc.rule.exceptions.ReturnNullFromCatchBlockRule.class,
      org.codenarc.rule.dry.DuplicateNumberLiteralRule.class,
      org.codenarc.rule.dry.DuplicateStringLiteralRule.class,
      org.codenarc.rule.basic.DeadCodeRule.class,
      org.codenarc.rule.basic.DoubleNegativeRule.class,
      org.codenarc.rule.basic.DuplicateCaseStatementRule.class,
      org.codenarc.rule.basic.RemoveAllOnSelfRule.class,
      // org.codenarc.rule.basic.SerialVersionUIDRule.class - removed in 0.14
      org.codenarc.rule.concurrency.SynchronizedOnGetClassRule.class,
      org.codenarc.rule.concurrency.UseOfNotifyMethodRule.class,
      // moved from basic in 0.16
      org.codenarc.rule.design.BooleanMethodReturnsNullRule.class,
      // moved from basic in 0.16
      org.codenarc.rule.design.ReturnsNullInsteadOfEmptyArrayRule.class,
      // moved from basic in 0.16
      org.codenarc.rule.design.ReturnsNullInsteadOfEmptyCollectionRule.class,
      org.codenarc.rule.convention.InvertedIfElseRule.class,
      org.codenarc.rule.groovyism.ExplicitArrayListInstantiationRule.class,
      org.codenarc.rule.groovyism.ExplicitCallToAndMethodRule.class,
      org.codenarc.rule.groovyism.ExplicitCallToCompareToMethodRule.class,
      org.codenarc.rule.groovyism.ExplicitCallToDivMethodRule.class,
      org.codenarc.rule.groovyism.ExplicitCallToEqualsMethodRule.class,
      org.codenarc.rule.groovyism.ExplicitCallToGetAtMethodRule.class,
      org.codenarc.rule.groovyism.ExplicitCallToLeftShiftMethodRule.class,
      org.codenarc.rule.groovyism.ExplicitCallToMinusMethodRule.class,
      org.codenarc.rule.groovyism.ExplicitCallToMultiplyMethodRule.class,
      org.codenarc.rule.groovyism.ExplicitCallToModMethodRule.class,
      org.codenarc.rule.groovyism.ExplicitCallToOrMethodRule.class,
      org.codenarc.rule.groovyism.ExplicitCallToPlusMethodRule.class,
      org.codenarc.rule.groovyism.ExplicitCallToPowerMethodRule.class,
      org.codenarc.rule.groovyism.ExplicitCallToRightShiftMethodRule.class,
      org.codenarc.rule.groovyism.ExplicitCallToXorMethodRule.class,
      org.codenarc.rule.groovyism.ExplicitHashMapInstantiationRule.class,
      org.codenarc.rule.groovyism.ExplicitHashSetInstantiationRule.class,
      org.codenarc.rule.groovyism.ExplicitLinkedListInstantiationRule.class,
      org.codenarc.rule.groovyism.ExplicitStackInstantiationRule.class,
      org.codenarc.rule.groovyism.ExplicitTreeSetInstantiationRule.class,
      org.codenarc.rule.groovyism.GStringAsMapKeyRule.class);

    insertRules(rules, "0.12", props, parametersByRule,
      org.codenarc.rule.unused.UnusedPrivateMethodParameterRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryBigDecimalInstantiationRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryBigIntegerInstantiationRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryBooleanInstantiationRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryCallForLastElementRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryCatchBlockRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryCollectCallRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryCollectionCallRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryConstructorRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryDoubleInstantiationRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryFloatInstantiationRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryGetterRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryGStringRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryInstantiationToGetClassRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryIntegerInstantiationRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryLongInstantiationRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryObjectReferencesRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryNullCheckRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryNullCheckBeforeInstanceOfRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryOverridingMethodRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryReturnKeywordRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryStringInstantiationRule.class,
      org.codenarc.rule.logging.LoggerForDifferentClassRule.class,
      org.codenarc.rule.logging.LoggingSwallowsStacktraceRule.class,
      org.codenarc.rule.logging.LoggerWithWrongModifiersRule.class,
      org.codenarc.rule.logging.MultipleLoggersRule.class,
      org.codenarc.rule.junit.UseAssertTrueInsteadOfNegationRule.class,
      org.codenarc.rule.junit.JUnitTestMethodWithoutAssertRule.class,
      org.codenarc.rule.exceptions.CatchArrayIndexOutOfBoundsExceptionRule.class,
      org.codenarc.rule.exceptions.CatchIndexOutOfBoundsExceptionRule.class,
      org.codenarc.rule.exceptions.MissingNewInThrowStatementRule.class,
      org.codenarc.rule.basic.ExplicitGarbageCollectionRule.class,
      // moved from basic in 0.16
      org.codenarc.rule.design.CompareToWithoutComparableRule.class,
      org.codenarc.rule.design.SimpleDateFormatMissingLocaleRule.class,
      org.codenarc.rule.design.AbstractClassWithoutAbstractMethodRule.class,
      org.codenarc.rule.design.CloseWithoutCloseableRule.class,
      org.codenarc.rule.design.ConstantsOnlyInterfaceRule.class,
      org.codenarc.rule.design.EmptyMethodInAbstractClassRule.class,
      org.codenarc.rule.design.FinalClassWithProtectedMemberRule.class,
      org.codenarc.rule.convention.ConfusingTernaryRule.class);

    insertRules(rules, "0.13", props, parametersByRule,
      // moved from basic in 0.16
      org.codenarc.rule.unnecessary.AddEmptyStringRule.class,
      // moved from basic in 0.16
      org.codenarc.rule.unnecessary.ConsecutiveLiteralAppendsRule.class,
      // moved from basic in 0.16
      org.codenarc.rule.unnecessary.ConsecutiveStringConcatenationRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryCallToSubstringRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryDefInMethodDeclarationRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryModOneRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryPublicModifierRule.class,
      org.codenarc.rule.unnecessary.UnnecessarySelfAssignmentRule.class,
      org.codenarc.rule.unnecessary.UnnecessarySemicolonRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryTransientModifierRule.class,
      org.codenarc.rule.junit.ChainedTestRule.class,
      org.codenarc.rule.junit.CoupledTestCaseRule.class,
      org.codenarc.rule.junit.UnnecessaryFailRule.class,
      org.codenarc.rule.exceptions.ExceptionExtendsErrorRule.class,
      org.codenarc.rule.basic.AssignmentInConditionalRule.class,
      org.codenarc.rule.basic.BooleanGetBooleanRule.class,
      org.codenarc.rule.basic.BrokenOddnessCheckRule.class,
      org.codenarc.rule.basic.EmptyInstanceInitializerRule.class,
      org.codenarc.rule.basic.EmptyMethodRule.class,
      org.codenarc.rule.basic.EmptyStaticInitializerRule.class,
      org.codenarc.rule.basic.IntegerGetIntegerRule.class,
      // org.codenarc.rule.basic.SerializableClassMustDefineSerialVersionUIDRule.class - removed in 0.14
      org.codenarc.rule.concurrency.BusyWaitRule.class,
      org.codenarc.rule.concurrency.DoubleCheckedLockingRule.class,
      org.codenarc.rule.concurrency.InconsistentPropertyLockingRule.class,
      org.codenarc.rule.concurrency.InconsistentPropertySynchronizationRule.class,
      org.codenarc.rule.concurrency.StaticCalendarFieldRule.class,
      org.codenarc.rule.concurrency.StaticDateFormatFieldRule.class,
      org.codenarc.rule.concurrency.StaticMatcherFieldRule.class,
      org.codenarc.rule.concurrency.SynchronizedOnBoxedPrimitiveRule.class,
      org.codenarc.rule.concurrency.SynchronizedOnStringRule.class,
      org.codenarc.rule.concurrency.SynchronizedReadObjectMethodRule.class,
      org.codenarc.rule.concurrency.SynchronizedOnReentrantLockRule.class,
      org.codenarc.rule.concurrency.VolatileArrayFieldRule.class,
      org.codenarc.rule.concurrency.WaitOutsideOfWhileLoopRule.class,
      org.codenarc.rule.groovyism.GroovyLangImmutableRule.class);

    insertRules(rules, "0.14", props, parametersByRule,
      org.codenarc.rule.security.NonFinalSubclassOfSensitiveInterfaceRule.class,
      org.codenarc.rule.security.InsecureRandomRule.class,
      org.codenarc.rule.security.FileCreateTempFileRule.class,
      org.codenarc.rule.security.SystemExitRule.class,
      org.codenarc.rule.security.ObjectFinalizeRule.class,
      org.codenarc.rule.security.JavaIoPackageAccessRule.class,
      org.codenarc.rule.security.UnsafeArrayDeclarationRule.class,
      org.codenarc.rule.security.PublicFinalizeMethodRule.class,
      org.codenarc.rule.security.NonFinalPublicFieldRule.class,
      org.codenarc.rule.jdbc.DirectConnectionManagementRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryFinalOnPrivateMethodRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryElseStatementRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryParenthesesForMethodCallWithClosureRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryPackageReferenceRule.class,
      org.codenarc.rule.junit.SpockIgnoreRestUsedRule.class,
      org.codenarc.rule.imports.ImportFromSunPackagesRule.class,
      org.codenarc.rule.imports.MisorderedStaticImportsRule.class,
      org.codenarc.rule.generic.IllegalPackageReferenceRule.class,
      org.codenarc.rule.exceptions.SwallowThreadDeathRule.class,
      org.codenarc.rule.basic.DuplicateMapKeyRule.class,
      org.codenarc.rule.basic.DuplicateSetValueRule.class,
      org.codenarc.rule.basic.EqualsOverloadedRule.class,
      org.codenarc.rule.basic.ForLoopShouldBeWhileLoopRule.class,
      org.codenarc.rule.basic.ClassForNameRule.class,
      org.codenarc.rule.basic.ComparisonOfTwoConstantsRule.class,
      org.codenarc.rule.basic.ComparisonWithSelfRule.class,
      org.codenarc.rule.serialization.SerialVersionUIDRule.class,
      org.codenarc.rule.serialization.SerializableClassMustDefineSerialVersionUIDRule.class,
      org.codenarc.rule.serialization.SerialPersistentFieldsRule.class,
      org.codenarc.rule.concurrency.StaticConnectionRule.class,
      org.codenarc.rule.concurrency.StaticSimpleDateFormatFieldRule.class,
      org.codenarc.rule.design.PublicInstanceFieldRule.class,
      org.codenarc.rule.design.StatelessSingletonRule.class,
      org.codenarc.rule.design.AbstractClassWithPublicConstructorRule.class,
      org.codenarc.rule.groovyism.ExplicitLinkedHashMapInstantiationRule.class,
      org.codenarc.rule.groovyism.ClosureAsLastMethodParameterRule.class);

    insertRules(rules, "0.15", props, parametersByRule,
      org.codenarc.rule.jdbc.JdbcConnectionReferenceRule.class,
      org.codenarc.rule.jdbc.JdbcResultSetReferenceRule.class,
      org.codenarc.rule.jdbc.JdbcStatementReferenceRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryDefInVariableDeclarationRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryDotClassRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryInstanceOfCheckRule.class,
      org.codenarc.rule.unnecessary.UnnecessarySubstringRule.class,
      org.codenarc.rule.grails.GrailsDomainHasToStringRule.class,
      org.codenarc.rule.grails.GrailsDomainHasEqualsRule.class,
      org.codenarc.rule.generic.IllegalClassReferenceRule.class,
      org.codenarc.rule.basic.BitwiseOperatorInConditionalRule.class,
      org.codenarc.rule.basic.HardCodedWindowsFileSeparatorRule.class,
      org.codenarc.rule.basic.RandomDoubleCoercedToZeroRule.class,
      org.codenarc.rule.basic.HardCodedWindowsRootDirectoryRule.class,
      org.codenarc.rule.formatting.BracesForClassRule.class,
      org.codenarc.rule.formatting.LineLengthRule.class,
      org.codenarc.rule.formatting.BracesForForLoopRule.class,
      org.codenarc.rule.formatting.BracesForIfElseRule.class,
      org.codenarc.rule.formatting.BracesForMethodRule.class,
      org.codenarc.rule.formatting.BracesForTryCatchFinallyRule.class,
      org.codenarc.rule.formatting.ClassJavadocRule.class,
      org.codenarc.rule.groovyism.AssignCollectionUniqueRule.class);

    insertRules(rules, "0.16", props, parametersByRule,
      org.codenarc.rule.unused.UnusedMethodParameterRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryDefInFieldDeclarationRule.class,
      org.codenarc.rule.naming.FactoryMethodNameRule.class,
      org.codenarc.rule.dry.DuplicateMapLiteralRule.class,
      org.codenarc.rule.dry.DuplicateListLiteralRule.class,
      org.codenarc.rule.design.BuilderMethodWithSideEffectsRule.class,
      org.codenarc.rule.convention.CouldBeElvisRule.class,
      org.codenarc.rule.convention.LongLiteralWithLowerCaseLRule.class,
      org.codenarc.rule.groovyism.AssignCollectionSortRule.class,
      org.codenarc.rule.groovyism.ConfusingMultipleReturnsRule.class,
      org.codenarc.rule.groovyism.GetterMethodCouldBePropertyRule.class,
      org.codenarc.rule.groovyism.UseCollectManyRule.class,
      org.codenarc.rule.groovyism.CollectAllIsDeprecatedRule.class,
      org.codenarc.rule.groovyism.UseCollectNestedRule.class);

    insertRules(rules, "0.17", props, parametersByRule,
      org.codenarc.rule.size.CrapMetricRule.class,
      org.codenarc.rule.basic.AssertWithinFinallyBlockRule.class,
      org.codenarc.rule.basic.ConstantAssertExpressionRule.class,
      org.codenarc.rule.basic.BrokenNullCheckRule.class,
      org.codenarc.rule.design.PrivateFieldCouldBeFinalRule.class,
      org.codenarc.rule.convention.ParameterReassignmentRule.class,
      org.codenarc.rule.convention.TernaryCouldBeElvisRule.class,
      org.codenarc.rule.convention.VectorIsObsoleteRule.class,
      org.codenarc.rule.convention.HashtableIsObsoleteRule.class);

    insertRules(rules, "0.18", props, parametersByRule,
      org.codenarc.rule.size.AbcMetricRule.class,
      org.codenarc.rule.junit.JUnitLostTestRule.class,
      org.codenarc.rule.junit.JUnitUnnecessaryThrowsExceptionRule.class,
      org.codenarc.rule.grails.GrailsDuplicateMappingRule.class,
      org.codenarc.rule.grails.GrailsDuplicateConstraintRule.class,
      org.codenarc.rule.exceptions.ExceptionNotThrownRule.class,
      org.codenarc.rule.formatting.SpaceAfterCommaRule.class,
      org.codenarc.rule.formatting.SpaceAfterSemicolonRule.class,
      org.codenarc.rule.formatting.SpaceAroundOperatorRule.class,
      org.codenarc.rule.formatting.SpaceBeforeOpeningBraceRule.class,
      org.codenarc.rule.formatting.SpaceAfterOpeningBraceRule.class,
      org.codenarc.rule.formatting.SpaceAfterClosingBraceRule.class,
      org.codenarc.rule.formatting.SpaceBeforeClosingBraceRule.class,
      org.codenarc.rule.formatting.SpaceAfterIfRule.class,
      org.codenarc.rule.formatting.SpaceAfterWhileRule.class,
      org.codenarc.rule.formatting.SpaceAfterForRule.class,
      org.codenarc.rule.formatting.SpaceAfterSwitchRule.class,
      org.codenarc.rule.formatting.SpaceAfterCatchRule.class,
      org.codenarc.rule.convention.IfStatementCouldBeTernaryRule.class);

    insertRules(rules, "0.19", props, parametersByRule,
      org.codenarc.rule.security.UnsafeImplementationAsMapRule.class,
      org.codenarc.rule.naming.ClassNameSameAsFilenameRule.class,
      org.codenarc.rule.junit.JUnitPublicFieldRule.class,
      org.codenarc.rule.junit.JUnitAssertEqualsConstantActualValueRule.class,
      org.codenarc.rule.grails.GrailsDomainReservedSqlKeywordNameRule.class,
      org.codenarc.rule.grails.GrailsDomainWithServiceReferenceRule.class,
      org.codenarc.rule.generic.IllegalClassMemberRule.class,
      org.codenarc.rule.basic.EmptyClassRule.class,
      org.codenarc.rule.serialization.EnumCustomSerializationIgnoredRule.class,
      org.codenarc.rule.concurrency.ThisReferenceEscapesConstructorRule.class,
      org.codenarc.rule.design.CloneWithoutCloneableRule.class,
      org.codenarc.rule.formatting.SpaceAroundClosureArrowRule.class,
      org.codenarc.rule.groovyism.GStringExpressionWithinStringRule.class);

    insertRules(rules, "0.20", props, parametersByRule,
      org.codenarc.rule.generic.IllegalStringRule.class,
      org.codenarc.rule.design.LocaleSetDefaultRule.class,
      org.codenarc.rule.formatting.SpaceAroundMapEntryColonRule.class,
      org.codenarc.rule.formatting.ClosureStatementOnOpeningLineOfMultipleLineClosureRule.class);

    insertRules(rules, "0.21", props, parametersByRule,
      org.codenarc.rule.unnecessary.UnnecessaryCastRule.class,
      org.codenarc.rule.unnecessary.UnnecessaryToStringRule.class,
      org.codenarc.rule.junit.JUnitPublicPropertyRule.class,
      org.codenarc.rule.imports.NoWildcardImportsRule.class,
      org.codenarc.rule.grails.GrailsMassAssignmentRule.class,
      org.codenarc.rule.generic.IllegalSubclassRule.class,
      org.codenarc.rule.exceptions.ExceptionExtendsThrowableRule.class,
      org.codenarc.rule.basic.MultipleUnaryOperatorsRule.class,
      org.codenarc.rule.design.ToStringReturnsNullRule.class,
      org.codenarc.rule.formatting.ConsecutiveBlankLinesRule.class,
      org.codenarc.rule.formatting.BlankLineBeforePackageRule.class,
      org.codenarc.rule.formatting.FileEndsWithoutNewlineRule.class,
      org.codenarc.rule.formatting.MissingBlankLineAfterImportsRule.class,
      org.codenarc.rule.formatting.MissingBlankLineAfterPackageRule.class,
      org.codenarc.rule.formatting.TrailingWhitespaceRule.class);

    insertRules(rules, "0.22", props, parametersByRule,
      org.codenarc.rule.unnecessary.UnnecessarySafeNavigationOperatorRule.class,
      org.codenarc.rule.naming.PackageNameMatchesFilePathRule.class,
      org.codenarc.rule.design.InstanceofRule.class,
      org.codenarc.rule.convention.NoDefRule.class);

    insertRules(rules, "0.23", props, parametersByRule,
      org.codenarc.rule.size.ParameterCountRule.class,
      org.codenarc.rule.design.NestedForLoopRule.class);

    insertRules(rules, "0.24", props, parametersByRule,
      org.codenarc.rule.naming.ClassNameSameAsSuperclassRule.class,
      org.codenarc.rule.naming.InterfaceNameSameAsSuperInterfaceRule.class,
      org.codenarc.rule.design.AssignmentToStaticFieldFromInstanceMethodRule.class);

    insertRules(rules, "0.25", props, parametersByRule,
      org.codenarc.rule.convention.TrailingCommaRule.class,
      org.codenarc.rule.convention.NoTabCharacterRule.class);

    return rules;
  }

  @SafeVarargs
  private static void insertRules(
    Multimap<RuleSet, Rule> rules,
    String version,
    Properties props,
    Map<String, AptResult> parametersByRule,
    Class<? extends AbstractRule>... ruleClasses) throws Exception {

    for (Class<? extends AbstractRule> ruleClass : ruleClasses) {
      rules.put(RuleSet.getCategory(ruleClass), new Rule(ruleClass, version, props, parametersByRule));
    }
  }

  private static Map<String, AptResult> retrieveRulesParameters() throws Exception {
    return new AptParser().parse(getRulesAptFile());
  }

  private static List<File> getRulesAptFile() {
    File aptDir = new File(RULES_APT_FILES_LOCATION);
    List<File> rulesAptFiles = Lists.newArrayList();
    if (aptDir.exists() && aptDir.isDirectory()) {
      File[] files = aptDir.listFiles();
      for (File file : files) {
        if (file.getName().startsWith("codenarc-rules-")) {
          rulesAptFiles.add(file);
        }
      }
    }
    return rulesAptFiles;
  }

  private void updateCounters(Rule rule) {
    count++;
    for (String tag : rule.tags) {
      Integer nbByTag = rulesByTags.get(tag);
      if (nbByTag == null) {
        nbByTag = 0;
      }
      rulesByTags.put(tag, nbByTag + 1);
    }

    String version = rule.version == null ? "legacy" : rule.version;
    Integer nbByVersion = rulesByVersion.get(version);
    if (nbByVersion == null) {
      nbByVersion = 0;
    }
    rulesByVersion.put(version, nbByVersion + 1);
  }

  private void resultsByVersion() {
    System.out.println("Rules by Version:");
    List<String> versions = Lists.newArrayList(rulesByVersion.keySet());
    Collections.sort(versions);
    for (String version : versions) {
      System.out.println("  - " + version + " : " + rulesByVersion.get(version));
    }
  }

  private void resultsByCategory() {
    System.out.println("Rules by category:");
    List<String> categories = Lists.newArrayList(rulesByTags.keySet());
    Collections.sort(categories);
    for (String category : categories) {
      System.out.println("  - " + category + " : " + rulesByTags.get(category));
    }
  }

  public void startPrintingRule(Rule rule) {
    if (duplications.contains(rule.key)) {
      System.out.println("Duplicated rule " + rule.key);
    } else {
      duplications.add(rule.key);
    }

    updateCounters(rule);

  }
}
