/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.language.base

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.EnableModelDsl
import org.gradle.util.TextUtil
import spock.lang.Unroll

class ComponentModelIntegrationTest extends AbstractIntegrationSpec {

    def "setup"() {
        EnableModelDsl.enable(executer)

        buildScript """
            interface CustomComponent extends ComponentSpec {}
            class DefaultCustomComponent extends BaseComponentSpec implements CustomComponent {}

            class ComponentTypeRules extends RuleSource {
                @ComponentType
                void registerCustomComponentType(ComponentTypeBuilder<CustomComponent> builder) {
                    builder.defaultImplementation(DefaultCustomComponent)
                }
            }

            apply type: ComponentTypeRules

            model {
                components {
                    main(CustomComponent)
                }
            }
        """
    }

    void withMainSourceSet() {
        buildFile << """
            interface CustomLanguageSourceSet extends LanguageSourceSet {
                String getData();
            }
            class DefaultCustomLanguageSourceSet extends BaseLanguageSourceSet implements CustomLanguageSourceSet {
                final String data = "foo"
            }

            class LanguageTypeRules extends RuleSource {
                @LanguageType
                void registerCustomLanguage(LanguageTypeBuilder<CustomLanguageSourceSet> builder) {
                    builder.defaultImplementation(DefaultCustomLanguageSourceSet)
                }
            }

            apply type: LanguageTypeRules

            model {
                components {
                    main {
                        sources {
                            main(CustomLanguageSourceSet)
                        }
                    }
                }
            }
        """
    }

    void withBinaries() {
        buildFile << """
            import org.gradle.model.collection.*

            interface CustomBinary extends BinarySpec {
                String getData();
            }
            class DefaultCustomBinary extends BaseBinarySpec implements CustomBinary {
                final String data = "bar"
            }

            class BinaryRules extends RuleSource {
                @BinaryType
                void registerCustomBinary(BinaryTypeBuilder<CustomBinary> builder) {
                    builder.defaultImplementation(DefaultCustomBinary)
                }

                @ComponentBinaries
                void addBinaries(CollectionBuilder<CustomBinary> binaries, CustomComponent component) {
                    binaries.create("main", CustomBinary)
                    binaries.create("test", CustomBinary)
                }
            }

            apply type: BinaryRules

            model {
                components {
                    test(CustomComponent)
                }
            }
        """
    }

    def "component sources and binaries containers are visible in model report"() {
        when:
        succeeds "model"

        then:
        output.contains(TextUtil.toPlatformLineSeparators("""
    components
        main
            binaries
            sources"""))
    }

    def "can reference sources container for a component in a rule"() {
        given:
        withMainSourceSet()
        buildFile << '''
            model {
                tasks {
                    create("printSourceNames") {
                        def sources = $("components.main.sources")
                        doLast {
                            println "names: ${sources*.name}"
                        }
                    }
                }
            }
        '''

        when:
        succeeds "printSourceNames"

        then:
        output.contains "names: [main]"
    }

    def "component sources container elements are visible in model report"() {
        given:
        withMainSourceSet()
        buildFile << """
            model {
                components {
                    main {
                        sources {
                            test(CustomLanguageSourceSet)
                        }
                    }
                    test(CustomComponent) {
                        sources {
                            test(CustomLanguageSourceSet)
                        }
                    }
                    foo(CustomComponent) {
                        sources {
                            bar(CustomLanguageSourceSet)
                        }
                    }
                }
            }
        """

        when:
        succeeds "model"

        then:
        output.contains(TextUtil.toPlatformLineSeparators("""
    components
        foo
            binaries
            sources
                bar
        main
            binaries
            sources
                main
                test
        test
            binaries
            sources
                test"""))
    }

    def "can reference sources container elements in a rule"() {
        given:
        withMainSourceSet()
        buildFile << '''
            model {
                tasks {
                    create("printSourceDisplayName") {
                        def sources = $("components.main.sources.main")
                        doLast {
                            println "sources display name: ${sources.displayName}"
                        }
                    }
                }
            }
        '''

        when:
        succeeds "printSourceDisplayName"

        then:
        output.contains "sources display name: DefaultCustomLanguageSourceSet 'main:main'"
    }

    def "can reference sources container elements using specialized type in a rule"() {
        given:
        withMainSourceSet()
        buildFile << '''
            import org.gradle.model.collection.*

            class TaskRules extends RuleSource {
                @Mutate
                void addPrintSourceDisplayNameTask(CollectionBuilder<Task> tasks, @Path("components.main.sources.main") CustomLanguageSourceSet sourceSet) {
                    tasks.create("printSourceData") {
                        doLast {
                            println "sources data: ${sourceSet.data}"
                        }
                    }
                }
            }

            apply type: TaskRules
        '''

        when:
        succeeds "printSourceData"

        then:
        output.contains "sources data: foo"
    }

    def "cannot remove source sets"() {
        given:
        withMainSourceSet()
        buildFile << '''
            import org.gradle.model.collection.*

            class SourceSetRemovalRules extends RuleSource {
                @Mutate
                void clearSourceSets(@Path("components.main.sources") NamedDomainObjectCollection<LanguageSourceSet> sourceSets) {
                    sourceSets.clear()
                }

                @Mutate
                void closeMainComponentSourceSetsForTasks(CollectionBuilder<Task> tasks, @Path("components.main.sources") NamedDomainObjectCollection<LanguageSourceSet> sourceSets) {
                }
            }

            apply type: SourceSetRemovalRules
        '''

        when:
        fails()

        then:
        failureHasCause("This collection does not support element removal.")
    }

    def "plugin can create component"() {
        when:
        buildFile << """
        class SomeComponentPlugin extends RuleSource {
            @Mutate
            void createComponent(ComponentSpecContainer specs) {
                specs.create("someCustomComponent", CustomComponent)
            }
        }
        apply plugin: SomeComponentPlugin
        """
        then:
        succeeds "model"

        and:
        output.contains(TextUtil.toPlatformLineSeparators("""    components
        main
            binaries
            sources
        someCustomComponent
            binaries
            sources
"""))

    }

    def "plugin can configure component with given name"() {
        given:
        withMainSourceSet()
        when:
        buildFile << """
        class SomeComponentPlugin extends RuleSource {
            @Mutate
            void addSourceSet(ComponentSpecContainer specs) {
                specs.named("main") {
                    sources {
                        bar(CustomLanguageSourceSet)
                    }
                }

            }
        }
        apply plugin: SomeComponentPlugin
        """
        then:
        succeeds "model"

        and:
        output.contains(TextUtil.toPlatformLineSeparators("""    components
        main
            binaries
            sources
                bar
                main"""))
    }

    def "plugin can apply component beforeEach / afterEach"() {
        when:
        buildFile << """
        class SomeComponentPlugin extends RuleSource {
            @Mutate
            void applyCustomizations(ComponentSpecContainer specs) {
                specs.create("newComponent", CustomComponent) {
                    println "creating \$it"
                }
                specs.afterEach {
                    println "afterEach \$it"
                }
                specs.beforeEach {
                    println "beforeEach \$it"
                }
            }
        }
        apply plugin: SomeComponentPlugin
        """
        then:
        succeeds "tasks"

        and:
        output.contains(TextUtil.toPlatformLineSeparators("""beforeEach DefaultCustomComponent 'newComponent'
creating DefaultCustomComponent 'newComponent'
afterEach DefaultCustomComponent 'newComponent'"""))

    }

    def "plugin can configure component with given type "() {
        given:
        withMainSourceSet()
        when:
        buildFile << """
        class SomeComponentPlugin extends RuleSource {
            @Mutate
            void applyCustomizations(ComponentSpecContainer specs) {
                specs.withType(CustomComponent) {
                    sources {
                        bar(CustomLanguageSourceSet)
                    }
                }
            }
        }
        apply plugin: SomeComponentPlugin
        """
        then:
        succeeds "model"

        and:
        output.contains(TextUtil.toPlatformLineSeparators("""    components
        main
            binaries
            sources
                bar
                main"""))

    }

    def "buildscript can create component"() {
        when:
        buildFile << """
        model {
            components {
                someCustomComponent(CustomComponent)
            }
        }
        """
        then:
        succeeds "model"

        and:
        output.contains(TextUtil.toPlatformLineSeparators("""someCustomComponent
            binaries
            sources"""))

    }

    def "buildscript can configure component with given name"() {
        given:
        withMainSourceSet()
        when:
        buildFile << """
        model {
            components {
                test(CustomComponent)

                named("main") {
                    sources {
                        bar(CustomLanguageSourceSet)
                    }
                }
            }
        }
        """
        then:
        succeeds "model"

        and:
        output.contains(TextUtil.toPlatformLineSeparators("""    components
        main
            binaries
            sources
                bar
                main
        test
            binaries
            sources
"""))

    }

    def "buildscript can apply component beforeEach / afterEach"() {
        given:
        withMainSourceSet()
        when:
        buildFile << """
        model {
            components {
               newComponent(CustomComponent){
                    println "creating \$it"
               }
               beforeEach {
                    println "beforeEach \$it"
               }

               afterEach {
                    println "afterEach \$it"
               }
            }
        }
        """
        then:
        succeeds "tasks"

        and:
        output.contains(TextUtil.toPlatformLineSeparators("""beforeEach DefaultCustomComponent 'newComponent'
creating DefaultCustomComponent 'newComponent'
afterEach DefaultCustomComponent 'newComponent'"""))

    }

    def "buildscript can configure component with given type "() {
        given:
        withMainSourceSet()
        when:
        buildFile << """
        model {
            components {
                withType(CustomComponent.class) {
                    sources {
                        bar(CustomLanguageSourceSet)
                    }
                }
            }
        }
        """
        then:
        succeeds "model"

        and:
        output.contains(TextUtil.toPlatformLineSeparators("""    components
        main
            binaries
            sources
                bar
                main"""))

    }

    def "reasonable error message when creating component with default implementation"() {
        when:
        buildFile << """
        model {
            components {
                another(DefaultCustomComponent)
            }
        }

        """
        then:
        fails "model"

        and:
        failureHasCause("Cannot create a DefaultCustomComponent because this type is not known to this collection. Known types are: CustomComponent")
    }

    def "reasonable error message when creating component with no implementation"() {
        when:
        buildFile << """
        interface AnotherCustomComponent extends ComponentSpec {}

        model {
            components {
                another(AnotherCustomComponent)
            }
        }

        """
        then:
        fails "model"

        and:
        failureHasCause("Cannot create a AnotherCustomComponent because this type is not known to this collection. Known types are: CustomComponent")

    }


    def "componentSpecContainer is groovy decorated when used in rules"() {
        given:
        withMainSourceSet()
        buildFile << '''
            import org.gradle.model.collection.*

            class ComponentSpecContainerRules extends RuleSource {
                @Mutate
                void addComponents(ComponentSpecContainer componentSpecs) {
                    componentSpecs.anotherCustom(CustomComponent) {
                    }
                }

                @Mutate
                void addComponentTasks(TaskContainer tasks, ComponentSpecContainer componentSpecs) {
                    tasks.create("printMainComponent") {
                        doLast{
                            //reference by name
                            println "Main component: " + componentSpecs.main.name
                        }

                    }
                }
            }

            apply type: ComponentSpecContainerRules
        '''

        when:
        succeeds "printMainComponent"
        then:
        output.contains("Main component: main")
    }

    @Unroll
    def "#projectionType is closed when used as input"() {
        given:
        withMainSourceSet()
        buildFile << """
            import org.gradle.model.collection.*

            class ComponentSpecContainerRules extends RuleSource {

                @Mutate
                void addComponentTasks(TaskContainer tasks, $projectionType componentSpecs) {
                    componentSpecs.all {
                        // some stuff here
                    }
                }
            }

            apply type: ComponentSpecContainerRules
        """

        when:
        fails "tasks"
        then:
        failureHasCause "Cannot add Mutate rule 'ComponentSpecContainerRules#addComponentTasks(org.gradle.api.tasks.TaskContainer, $fullQualified) > all()' for model element 'components.main'"

        where:
        projectionType                     | fullQualified
        "CollectionBuilder<ComponentSpec>" | "org.gradle.model.collection.CollectionBuilder<org.gradle.platform.base.ComponentSpec>"
        "ComponentSpecContainer"           | "org.gradle.platform.base.ComponentSpecContainer"
    }

    def "component binaries container elements are visible in model report"() {
        given:
        withBinaries()

        when:
        succeeds "model"

        then:
        output.contains(TextUtil.toPlatformLineSeparators("""
    components
        main
            binaries
                main
                test
            sources
        test
            binaries
                main
                test
            sources"""))
    }

    def "can reference binaries container for a component in a rule"() {
        given:
        withBinaries()
        buildFile << '''
            model {
                tasks {
                    create("printBinaryNames") {
                        def binaries = $("components.main.binaries")
                        doLast {
                            println "names: ${binaries*.name}"
                        }
                    }
                }
            }
        '''

        when:
        succeeds "printBinaryNames"

        then:
        output.contains "names: [main, test]"
    }

    def "can reference binaries container elements using specialized type in a rule"() {
        given:
        withBinaries()
        buildFile << '''
            import org.gradle.model.collection.*

            class TaskRules extends RuleSource {
                @Mutate
                void addPrintSourceDisplayNameTask(CollectionBuilder<Task> tasks, @Path("components.main.binaries.main") DefaultCustomBinary binary) {
                    tasks.create("printBinaryData") {
                        doLast {
                            println "binary data: ${binary.data}"
                        }
                    }
                }
            }

            apply type: TaskRules
        '''

        when:
        succeeds "printBinaryData"

        then:
        output.contains "binary data: bar"
    }

    def "cannot remove binaries"() {
        given:
        withBinaries()
        buildFile << '''
            import org.gradle.model.collection.*

            class BinariesRemovalRules extends RuleSource {
                @Mutate
                void clearSourceSets(@Path("components.main.binaries") NamedDomainObjectCollection<BinarySpec> binaries) {
                    binaries.clear()
                }

                @Mutate
                void closeMainComponentBinariesForTasks(CollectionBuilder<Task> tasks, @Path("components.main.binaries") NamedDomainObjectCollection<BinarySpec> binaries) {
                }
            }

            apply type: BinariesRemovalRules
        '''

        when:
        fails()

        then:
        failureHasCause("This collection does not support element removal.")
    }
}