package org.apache.commons.mail;
import static org.junit.Assert.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.List;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import static org.easymock.EasyMock.*;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.modules.junit4.PowerMockRunner;

public class EmailTest {

    private static final String[] TEST_EMAILS = { 
        "alice@example.com", 
        "bob@work.org", 
        "user123456789@longdomainnameexample.co.uk" 
    };

    private String[] testValidChars = { " ", "z", "Z", "\u2603", "9876543210", "12345678901234567890", "\r\n" };

    private static final String testEmail = "testuser@mail.com";

    private EmailConcrete email;

    private Session mySession;

    private Date testDate;

    private static final int SOCKET_TIMEOUT_MS = 60000;

    @Before
    public void setUpEmailTest() throws Exception {
        email = new EmailConcrete();

        Calendar calendar = Calendar.getInstance();
        calendar.set(2025, Calendar.APRIL, 15);
        testDate = calendar.getTime();
    }

    @After
    public void tearDownEmailTest() throws Exception {}

    @Test
    public void testAddBcc() throws Exception {
        email.addBcc(TEST_EMAILS);
        assertEquals(3, email.getBccAddresses().size());
    }

    @Test
    public void testAddCc() throws Exception {
        email.addCc("ccuser1@mail.com");
        email.addCc("ccuser2@mail.org");
        assertEquals(2, email.getCcAddresses().size());
    }

    @Test
    public void testAddHeader() throws Exception {
        email.addHeader("From", testEmail);
        email.addHeader("To", "recipient@domain.edu");

        assertEquals(testEmail, email.getHeaders().get("From"));
        assertEquals("recipient@domain.edu", email.getHeaders().get("To"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddHeaderNullName() throws Exception {
        email.addHeader("", testEmail);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddHeaderNullValue() throws Exception {
        email.addHeader("From", "");
    }

    @Test
    public void testAddReplyTo() throws Exception {
        email.addReplyTo("replyto@mail.net", "Reply User");
        List<InternetAddress> replyToAddresses = email.getReplyToAddresses();

        assertNotNull(replyToAddresses);
        assertEquals(1, replyToAddresses.size());
        InternetAddress firstAddress = replyToAddresses.get(0);

        assertEquals("replyto@mail.net", firstAddress.getAddress());
        assertEquals("Reply User", firstAddress.getPersonal());
    }

    @Test(expected = RuntimeException.class)
    public void testBuildMimeMessage() throws Exception {
        try {
            email.setHostName("testhost.local");
            email.setSmtpPort(9999);
            email.setFrom("sender@host.com");
            email.addTo("receiver@host.com");
            email.setSubject("Initial Subject");
            email.setCharset("UTF-8");
            email.setContent("Email body content", "text/plain");
            email.buildMimeMessage();
            email.buildMimeMessage(); // Second call triggers exception
        } catch (RuntimeException re) {
            String message = "The MimeMessage is already built.";
            assertEquals(message, re);
            throw re;
        }
    }

    @Test
    public void testSuccessfulBuildMimeMessage() throws Exception {
        email.setHostName("testhost.local");
        email.setSmtpPort(2525);
        email.setFrom("sender@demo.com");
        email.addTo("receiver@demo.com");
        email.setSubject("Successful Build Test");
        email.setContent("This is a test email.", "text/plain");

        email.buildMimeMessage();

        assertNotNull(email.getMimeMessage());
    }

    @Test(expected = EmailException.class)
    public void testBuildMimeMessageWithoutSender() throws Exception {
        email.setHostName("testhost.local");
        email.setSmtpPort(2525);
        email.addTo("user@nowhere.com");
        email.setSubject("Missing Sender");
        email.setContent("Body without sender.", "text/plain");

        email.buildMimeMessage();
    }

    @Test(expected = EmailException.class)
    public void testBuildMimeMessageWithoutRecipients() throws Exception {
        email.setHostName("testhost.local");
        email.setSmtpPort(2525);
        email.setFrom("sender@nowhere.com");
        email.setSubject("Missing Recipients");
        email.setContent("Body without recipient.", "text/plain");

        email.buildMimeMessage();
    }

    @Test
    public void testBuildMimeMessageWithHeaders() throws Exception {
        email.setHostName("localhost");
        email.setSmtpPort(2525);
        email.setFrom("from@site.com");
        email.addTo("to@site.com");
        email.setSubject("Header Example");
        email.addHeader("Subject", "Overridden Subject");
        email.buildMimeMessage();
        assertEquals("Overridden Subject", email.getMimeMessage().getHeader("Subject")[0]);
    }

    @Test
    public void testGetHostNameFromSession() throws Exception {
        Email e = new EmailConcrete();
        e.setHostName(null);

        Properties properties = new Properties();
        properties.setProperty("mail.host", "mail.fakehost.com");
        Session session = Session.getInstance(properties);

        e.setMailSession(session);

        assertEquals("mail.fakehost.com", e.getHostName());
    }

    @Test
    public void testGetHostName() throws Exception {
        Email e = new EmailConcrete();
        e.setHostName("customhost.net");
        assertEquals("customhost.net", e.getHostName());
    }

    @Test
    public void testGetHostNameFromHostNameField() throws Exception {
        Email email = new EmailConcrete();
        email.setMailSession(null);
        email.setHostName("host.backup.com");
        assertEquals("host.backup.com", email.getHostName());
    }

    @Test
    public void testGetHostNameReturnsNull() throws Exception {
        Email email = new EmailConcrete();
        email.setMailSession(null);
        email.setHostName(null);
        assertNull(email.getHostName());
    }

    @Test
    public void testGetMailSessionCreateSession() throws Exception {
        Session aSession = email.getMailSession();
        assertNotNull(aSession);
    }

    @Test
    public void testEmptySentDate() throws Exception {
        email.setSentDate(null);
        Date currentDate = new Date();
        Date sentDate = email.getSentDate();
        assertTrue(Math.abs(currentDate.getTime() - sentDate.getTime()) < 1000);
    }

    @Test
    public void testGetSentDate() throws Exception {
        email.setSentDate(testDate);
        assertEquals(testDate, email.getSentDate());
    }

    @Test
    public void testGetSocketConnectionTimeoutDefaultValue() {
        assertEquals(EmailConstants.SOCKET_TIMEOUT_MS, email.getSocketConnectionTimeout());
    }

    @Test
    public void testSetFrom() throws Exception {
        email.setFrom(testEmail);
        assertEquals(testEmail, email.getFromAddress());
    }
}
