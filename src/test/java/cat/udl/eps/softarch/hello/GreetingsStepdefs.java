package cat.udl.eps.softarch.hello;

import cat.udl.eps.softarch.hello.config.ApplicationConfig;
import cat.udl.eps.softarch.hello.model.Greeting;
import cat.udl.eps.softarch.hello.model.User;
import cat.udl.eps.softarch.hello.repository.GreetingRepository;
import cat.udl.eps.softarch.hello.repository.UserRepository;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Created by http://rhizomik.net/~roberto/
 */

@WebAppConfiguration
@ContextConfiguration(classes = ApplicationConfig.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class GreetingsStepdefs {

    static DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    GreetingRepository greetingRepository;
    @Autowired
    UserRepository userRepository;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc       mockMvc;
    private ResultActions result;

    @Before
    public void setup() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(this.wac)
                .build();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Given("^the greetings repository has the following greetings:$")
    public void the_greetings_repository_has_the_following_greetings(List<Greeting> greetings) throws Throwable {
        Long index = 1L;
        for (Greeting g: greetings) {
            if (!greetingRepository.exists(index))
                greetingRepository.save(g);
            else if (!greetingRepository.findOne(index).getMessage().equals(g.getMessage())) {
                Greeting toBeUpdated = greetingRepository.findOne(index);
                toBeUpdated.setMessage(g.getMessage());
                greetingRepository.save(toBeUpdated);
            }
            index++;
        }
    }

    @Given("^the greetings repository has no greetings$")
    public void the_greetings_repository_has_no_greetings() throws Throwable {
        greetingRepository.deleteAll();
    }

    @Given("^the users repository has the following users:$")
    public void the_users_repository_has_the_following_users(List<User> users) throws Throwable {
        for (User u: users) {
            if (!userRepository.exists(u.getUsername()))
                userRepository.save(u);
        }
    }

    @When("^the client request the list of greetings$")
    public void the_client_request_the_list_of_greetings() throws Throwable {
        result = mockMvc.perform(get("/greetings")
                .accept(MediaType.APPLICATION_JSON));
    }

    @Then("^the response is a list containing (\\d+) greetings$")
    public void the_response_is_a_list_containing_greetings(int length) throws Throwable {
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$._embedded.greetings", hasSize(length)));
    }

    @And("^one greeting has id (\\d+) and content \"([^\"]*)\"$")
    public void one_greeting_has_id_and_content(int id, String content) throws Throwable {
        result.andExpect(jsonPath("$._embedded.greetings[?(@._links.self.href=='http://localhost/greetings/"+id+"')].message").value(hasItem(content)));
    }

    @When("^the client requests greeting with id (\\d+)$")
    public void the_client_requests_greeting_with_id(int id) throws Throwable {
        result = mockMvc.perform(get("/greetings/{id}", id)
                            .accept(MediaType.APPLICATION_JSON));
    }

    @Then("^the response is a greeting with id (\\d+) and content \"([^\"]*)\"$")
    public void the_response_is_a_greeting_with_id_and_content(int id, String content) throws Throwable {
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$._links.self.href").value(is("http://localhost/greetings/"+id)))
                .andExpect(jsonPath("$.message").value(is(content)));
    }

    @Given("^greeting with id (\\d+) doesn't exist$")
    public void greeting_with_id_doesn_t_exist(Long id) throws Throwable {
        assertFalse(greetingRepository.exists(id));
    }

    @Then("^the response is status code (\\d+)$")
    public void the_response_is_status_code(int statusCode) throws Throwable {
        result.andExpect(status().is(statusCode));
    }

    @And("^error message contains \"([^\"]*)\"$")
    public void error_message_contains(String errorMessage) throws Throwable {
        result.andExpect(jsonPath("$.message", containsString(errorMessage)));
    }

    @When("^the client creates a greeting with content \"([^\"]*)\", email \"([^\"]*)\" and date \"([^\"]*)\"$")
    public void the_client_creates_a_greeting_with_content_email_and_date(String content, String email, Date date) throws Throwable {
        result = mockMvc.perform(post("/greetings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"message\": \"" + content + "\"" +
                                    ", \"email\": \"" + email + "\"" +
                                    ", \"date\": \"" + df.format(date) + "\" }")
                            .accept(MediaType.APPLICATION_JSON));
    }

    @And("^header \"([^\"]*)\" points to a greeting with content \"([^\"]*)\"$")
    public void header_points_to_a_greeting_with_content(String header, String content) throws Throwable {
        String location = result.andReturn().getResponse().getHeader(header);
        result = mockMvc.perform(get(location)
                            .accept(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message", is(content)));
    }

    @When("^the client updates greeting with id (\\d+) with content \"([^\"]*)\", email \"([^\"]*)\" and date \"([^\"]*)\"$")
    public void the_client_updates_greeting_with_id_with_content_email_and_date(int id, String content, String email, Date date) throws Throwable {
        result = mockMvc.perform(put("/greetings/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"message\": \"" + content + "\"" +
                        ", \"email\": \"" + email + "\"" +
                        ", \"date\": \"" + df.format(date) + "\" }")
                .accept(MediaType.APPLICATION_JSON));
    }

    @When("^the client deletes greeting with id (\\d+)$")
    public void the_client_deletes_greeting_with_id(int id) throws Throwable {
        result = mockMvc.perform(delete("/greetings/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON));
    }

    @And("^the response is empty$")
    public void the_response_is_empty() throws Throwable {
        result.andExpect(content().string(""));
    }
}
