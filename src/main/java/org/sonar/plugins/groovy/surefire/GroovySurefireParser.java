/*
 * Sonar Groovy Plugin
 * Copyright (C) 2010 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.groovy.surefire;

import org.sonar.api.BatchExtension;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.TestCase;
import org.sonar.api.utils.ParsingUtils;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.StaxParser;
import org.sonar.plugins.groovy.foundation.Groovy;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.plugins.groovy.surefire.data.SurefireStaxHandler;
import org.sonar.plugins.groovy.surefire.data.UnitTestClassReport;
import org.sonar.plugins.groovy.surefire.data.UnitTestIndex;
import org.sonar.plugins.groovy.surefire.data.UnitTestResult;

/**
 * @author iwarapter
 */
public class GroovySurefireParser  implements BatchExtension {
	private static final Logger LOGGER = LoggerFactory.getLogger(GroovySurefireParser.class);
	private final ResourcePerspectives perspectives;
	private final FileSystem fs;
	private final Project project;

	public GroovySurefireParser(ResourcePerspectives perspectives, Project project, FileSystem fs) {
		this.perspectives = perspectives;
		this.project = project;
		this.fs = fs;
	}

	public void collect(SensorContext context, File reportsDir) {
		File[] xmlFiles = getReports(reportsDir);
		if (xmlFiles.length > 0) {
			parseFiles(context, xmlFiles);
		}
	}

	private File[] getReports(File dir) {
		if (dir == null) {
			return new File[0];
		} else if (!dir.isDirectory()) {
			LOGGER.warn("Reports path not found: " + dir.getAbsolutePath());
			return new File[0];
		}
		File[] unitTestResultFiles = findXMLFilesStartingWith(dir, "TEST-");
		if (unitTestResultFiles.length == 0) {
			// maybe there's only a test suite result file
			unitTestResultFiles = findXMLFilesStartingWith(dir, "TESTS-");
		}
		return unitTestResultFiles;
	}

	private File[] findXMLFilesStartingWith(File dir, final String fileNameStart) {
		return dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(fileNameStart) && name.endsWith(".xml");
			}
		});
	}

	private void parseFiles(SensorContext context, File[] reports) {
		UnitTestIndex index = new UnitTestIndex();
		parseFiles(reports, index);
		sanitize(index);
		save(index, context);
	}

	private void parseFiles(File[] reports, UnitTestIndex index) {
		SurefireStaxHandler staxParser = new SurefireStaxHandler(index);
		StaxParser parser = new StaxParser(staxParser, false);
		for (File report : reports) {
			try {
				parser.parse(report);
			} catch (XMLStreamException e) {
				throw new SonarException("Fail to parse the Surefire report: " + report, e);
			}
		}
	}

	private void sanitize(UnitTestIndex index) {
		for (String classname : index.getClassnames()) {
			if (StringUtils.contains(classname, "$")) {
				// Surefire reports classes whereas sonar supports files
				String parentClassName = StringUtils.substringBefore(classname, "$");
				index.merge(classname, parentClassName);
			}
		}
	}

	private void save(UnitTestIndex index, SensorContext context) {
		long negativeTimeTestNumber = 0;
		for (Map.Entry<String, UnitTestClassReport> entry : index.getIndexByClassname().entrySet()) {
			UnitTestClassReport report = entry.getValue();
			if (report.getTests() > 0) {
				negativeTimeTestNumber += report.getNegativeTimeTestNumber();
				Resource resource = getUnitTestResource(entry.getKey());
				if (resource != null) {
					save(report, resource, context);
				} else {
					LOGGER.warn("Resource not found: {}", entry.getKey());
				}
			}
		}
		if (negativeTimeTestNumber > 0) {
			LOGGER.warn("There is {} test(s) reported with negative time by surefire, total duration may not be accurate.", negativeTimeTestNumber);
		}
	}

	private void save(UnitTestClassReport report, Resource resource, SensorContext context) {
		double testsCount = report.getTests() - report.getSkipped();
		saveMeasure(context, resource, CoreMetrics.SKIPPED_TESTS, report.getSkipped());
		saveMeasure(context, resource, CoreMetrics.TESTS, testsCount);
		saveMeasure(context, resource, CoreMetrics.TEST_ERRORS, report.getErrors());
		saveMeasure(context, resource, CoreMetrics.TEST_FAILURES, report.getFailures());
		saveMeasure(context, resource, CoreMetrics.TEST_EXECUTION_TIME, report.getDurationMilliseconds());
		double passedTests = testsCount - report.getErrors() - report.getFailures();
		if (testsCount > 0) {
			double percentage = passedTests * 100d / testsCount;
			saveMeasure(context, resource, CoreMetrics.TEST_SUCCESS_DENSITY, ParsingUtils.scaleValue(percentage));
		}
		saveResults(resource, report);
	}

	protected void saveResults(Resource testFile, UnitTestClassReport report) {
		for (UnitTestResult unitTestResult : report.getResults()) {
			MutableTestPlan testPlan = perspectives.as(MutableTestPlan.class, testFile);
			if (testPlan != null) {
				testPlan.addTestCase(unitTestResult.getName())
						.setDurationInMs(Math.max(unitTestResult.getDurationMilliseconds(), 0))
						.setStatus(TestCase.Status.of(unitTestResult.getStatus()))
						.setMessage(unitTestResult.getMessage())
						.setType(TestCase.TYPE_UNIT)
						.setStackTrace(unitTestResult.getStackTrace());
			}
		}
	}

	protected Resource getUnitTestResource(String classKey) {
		String fileName = StringUtils.replace(classKey, ".", "/") + ".groovy";
		FilePredicates p = fs.predicates();
		FilePredicate searchPredicate = p.and(p.and(p.hasLanguage(Groovy.KEY), p.hasType(InputFile.Type.TEST)), p.matchesPathPattern("**/" + fileName));
		if (fs.hasFiles(searchPredicate)) {
			File testFile = fs.files(searchPredicate).iterator().next();
			return org.sonar.api.resources.File.fromIOFile(testFile, project);
		} else {
			return null;
		}
	}

	private void saveMeasure(SensorContext context, Resource resource, Metric metric, double value) {
		if (!Double.isNaN(value)) {
			context.saveMeasure(resource, metric, value);
		}
	}
}