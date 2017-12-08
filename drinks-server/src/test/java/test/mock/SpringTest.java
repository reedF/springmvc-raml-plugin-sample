package test.mock;

import static guru.nidi.ramltester.junit.RamlMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.phoenixnap.oss.sample.server.ServerLauncher;

import guru.nidi.ramltester.RamlDefinition;
import guru.nidi.ramltester.RamlLoaders;
import guru.nidi.ramltester.SimpleReportAggregator;
import guru.nidi.ramltester.core.RamlReport;
import guru.nidi.ramltester.junit.ExpectedUsage;
import guru.nidi.ramltester.junit.RamlMatchers;

/**
 * using 
 *      <dependency>
            <groupId>guru.nidi.raml</groupId>
            <artifactId>raml-tester</artifactId>
            <version>0.9.1</version>
            <scope>test</scope>
        </dependency>
   to validate raml
 * @author reed
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = ServerLauncher.class)
public class SpringTest {
    private static String path = System.getProperty("user.dir") + "/target/classes/public/raml";
    private static RamlDefinition api =
            // RamlLoaders.fromClasspath(ServerLauncher.class).load("public/raml/api.raml")
            // .assumingBaseUri("http://localhost:8080");
            RamlLoaders.fromFile(path).load("api-1.0.raml")
                    .assumingBaseUri("http://localhost:8080");
    private static SimpleReportAggregator aggregator = new SimpleReportAggregator();
    private final static JsonSchemaFactory factory = JsonSchemaFactory.byDefault();

    /**
     * 校验raml文件中的所有resources, query parameters, form parameters, headers and response
     * 都至少使用一次
     */
    // @ClassRule
    public static ExpectedUsage expectedUsage = new ExpectedUsage(aggregator);

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void testDrinks() throws Exception {
        Assert.assertThat(api.validate(), RamlMatchers.validates());
        // Assert.assertThat(api.validate(), hasNoViolations());

        MvcResult mvcResult =
                mockMvc.perform(get("/drinks/fanta").accept(MediaType.parseMediaType("application/json")))
                        .andExpect(status().isOk()).andReturn();
        api.matches().aggregating(aggregator);
        String output = mvcResult.getResponse().getContentAsString();
        ProcessingReport report = validatorSchema("drink-get.json", output);
        Assert.assertTrue(report.isSuccess());
    }

    @Test
    public void testWithMvcResult() throws Exception {
        MvcResult mvcResult = mockMvc
                .perform(get("/healthCheck").accept(MediaType.parseMediaType("application/json")))
                .andExpect(status().isOk()).andReturn();

        String output = mvcResult.getResponse().getContentAsString();
        ProcessingReport report = validatorSchema("health-get.json", output);
        Assert.assertTrue(report.isSuccess());

        RamlReport ramlreport = aggregator.addReport(api.testAgainst(mvcResult));
        // Assert.assertThat(ramlreport, checks());
    }

    /**
     * validate instance and Schema,here including two functions. as follows:
     * first： the Draft v4 will check the syntax both of schema and instance.
     * second： instance validation.
     * 
     * @param mainSchema
     * @param instance
     * @return
     * @throws IOException
     * @throws ProcessingException
     */
    public static ProcessingReport validatorSchema(String mainSchema, String instance)
            throws IOException, ProcessingException {
        JsonNode mainNode = JsonLoader.fromPath(path + "/schemas/" + mainSchema);
        JsonNode instanceNode = JsonLoader.fromString(instance);
        JsonSchema schema = factory.getJsonSchema(mainNode);
        ProcessingReport processingReport = schema.validate(instanceNode);

        return processingReport;
    }
}
