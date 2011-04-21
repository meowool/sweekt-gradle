/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.plugins.ide.eclipse

import org.gradle.integtests.fixtures.TestResources
import org.junit.Rule
import org.junit.Test

/**
 * Author: Szczepan Faber
 */
class EclipseProjectIntegrationTest extends AbstractEclipseIntegrationTest {

    @Rule
    public final TestResources testResources = new TestResources()

    String content

    @Test
    void allowsConfiguringEclipseProject() {
        //when
        runEclipseTask """
apply plugin: 'java'
apply plugin: 'eclipse'

eclipse {
  project {
    name = 'someBetterName'
    comment = 'a test project'

    referencedProjects = ['some referenced project'] as Set
    referencedProjects 'some cool project'

    natures = ['test.groovy.nature']
    natures 'test.java.nature'

    buildCommand 'buildThisLovelyProject'
    buildCommand argumentFoo: 'a foo argument', 'buildWithTheArguments'

    linkedResource name: 'linkToFolderFoo', type: 'aFolderFoo', location: '/test/folders/foo'
    linkedResource name: 'linkToUriFoo', type: 'aFooUri', locationUri: 'http://test/uri/foo'

    file {
      withXml { it.asNode().appendNode('motto', 'Stay happy!') }
    }
  }

  jdt {
    sourceCompatibility = 1.4
    targetCompatibility = 1.3
  }
}
        """

        //then
        content = getFile([:], '.project').text
        println content

        def dotProject = parseProjectFile()
        assert dotProject.name.text() == 'someBetterName'
        assert dotProject.comment.text() == 'a test project'

        contains('some referenced project', 'some cool project')
        contains('test.java.nature', 'test.groovy.nature')
        contains('buildThisLovelyProject', 'argumentFoo', 'a foo argument', 'buildWithTheArguments')

        contains('linkToFolderFoo', 'aFolderFoo', '/test/folders/foo')
        contains('linkToUriFoo', 'aFooUri', 'http://test/uri/foo')

        contains('<motto>Stay happy!</motto>')

        def jdt = parseJdtFile()
        assert jdt.contains('targetPlatform=1.3')
        assert jdt.contains('source=1.4')
    }

    @Test
    void enablesBeforeAndWhenHooksForProject() {
        //given
        def project = file('.project')
        project << '''<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
	<name>root</name>
	<comment/>
	<projects/>
	<natures>
		<nature>org.eclipse.jdt.core.javanature</nature>
		<nature>some.bad.nature.one</nature>
	</natures>
	<buildSpec>
		<buildCommand>
			<name>org.eclipse.jdt.core.javabuilder</name>
			<arguments/>
		</buildCommand>
	</buildSpec>
	<linkedResources/>
</projectDescription>'''

        //when
        runEclipseTask """
apply plugin: 'java'
apply plugin: 'eclipse'

eclipse {
  project {
    file {
      beforeMerged { it.natures << 'some.bad.nature.two' }
      whenMerged {
        assert it.natures.contains('some.bad.nature.one')
        assert it.natures.contains('some.bad.nature.two')

        it.natures = ['some.cool.nature']
      }
    }
  }
}
        """

        content = getFile([:], '.project').text
        //then

        assert content.contains('some.cool.nature')
        assert !content.contains('some.bad.nature')
    }

    @Test
    void enablesBeforeAndWhenHooksForJdt() {
        //given
        def jdtFile = file('.settings/org.eclipse.jdt.core.prefs')
        jdtFile << '''
org.eclipse.jdt.core.compiler.codegen.targetPlatform=1.3
'''

        //when
        runEclipseTask """
apply plugin: 'java'
apply plugin: 'eclipse'

hooks = []

eclipse {

  jdt {
    file {
      beforeMerged {
        hooks << 'beforeMerged'
      }
      whenMerged {
        hooks << 'whenMerged'
        assert '1.1' != it.targetCompatibility.toString()
        it.targetCompatibility = JavaVersion.toVersion('1.1')
      }
    }
  }
}

eclipseJdt.doLast() {
  assert hooks == ['beforeMerged', 'whenMerged']
}
        """

        def jdt = parseJdtFile()
        println jdt

        //then
        assert jdt.contains('targetPlatform=1.1')
    }

    protected def contains(String ... contents) {
        contents.each { assert content.contains(it)}
    }
}
