package com.github.sergueik.selenium;
/**
 * Copyright 2022 Serguei Kouzmine
 */

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;

import com.google.gson.JsonSyntaxException;

/**
 * Selected test scenarios for Selenium 4 Chrome Developer Tools bridge
 * 
 * https://chromedevtools.github.io/devtools-protocol/tot/Page/#method-printToPDF
 */

// NOTE: not extending BaseDevToolsTest
public class PrintToPDFCDPTest {

	private static Map<String, Object> result = new HashMap<>();
	private static Map<String, Object> params = new HashMap<>();

	private static String command = null;
	private static String magic = null;
	private static boolean runHeadless = true;

	private static String body = null;
	private static boolean landscape = false;
	private static boolean displayHeaderFooter = false;
	private static boolean printBackground = false;
	private static boolean preferCSSPageSize = false;
	private static String transferMode = "ReturnAsBase64";
	private static int scale = 1;
	private static String filename = "result1.pdf";
	private static String osName = Utils.getOSName();
	private static ChromiumDriver driver;
	private static DevTools chromeDevTools;
	private static String baseURL = "https://www.wikipedia.org";

	@BeforeClass
	public static void setUp() throws Exception {

		if (System.getenv().containsKey("HEADLESS")
				&& System.getenv("HEADLESS").matches("(?:true|yes|1)")) {
			runHeadless = true;
		}
		// force the headless flag to be true to support Unix console execution
		if (!(Utils.getOSName().equals("windows"))
				&& !(System.getenv().containsKey("DISPLAY"))) {
			runHeadless = true;
		}
		System
				.setProperty("webdriver.chrome.driver",
						Paths.get(System.getProperty("user.home"))
								.resolve("Downloads").resolve(osName.equals("windows")
										? "chromedriver.exe" : "chromedriver")
								.toAbsolutePath().toString());

		if (runHeadless) {
			ChromeOptions options = new ChromeOptions();
			options.addArguments("--headless", "--disable-gpu");
			driver = new ChromeDriver(options);
		} else {
			driver = new ChromeDriver();
		}
		Utils.setDriver(driver);

		chromeDevTools = ((HasDevTools) driver).getDevTools();

		chromeDevTools.createSession();
		// TODO: switch to
		// chromeDevTools.createSessionIfThereIsNotOne();
	}

	@AfterClass
	public static void afterClass() {
		if (driver != null) {
			driver.quit();
		}
	}

	@After
	public void afterTest() {
		new File(System.getProperty("user.dir") + "/" + filename).delete();
		driver.get("about:blank");
	}

	@Before
	public void beforeTest() {
		// Arrange
		driver.get(baseURL);
	}

	//
	@Test
	public void test1() {
		command = "Page.printToPDF";
		params = new HashMap<>();
		params.put("landscape", landscape);
		params.put("displayHeaderFooter", displayHeaderFooter);
		params.put("printBackground", printBackground);
		params.put("preferCSSPageSize", preferCSSPageSize);

		params.put("transferMode", transferMode);

		// "paperHeight",
		// "paperWidth",
		// "marginTop",
		// "marginBottom",
		// "marginLeft",
		// "marginRight",
		// "pageRanges",
		// "headerTemplate",
		// "footerTemplate",
		// "preferCSSPageSize",
		// Act

		result = driver.executeCdpCommand(command, params);

		assertThat(result, notNullValue());
		assertThat(result, hasKey("data"));

		try {
			body = new String(
					Base64.decodeBase64(((String) result.get("data")).getBytes("UTF8")));
			assertThat(body, notNullValue());
			magic = body.substring(0, 9);
			assertThat(magic, containsString("%PDF"));
			writeToFile(Base64.decodeBase64((String) result.get("data")), filename);

			PDF pdf = new PDF(
					new File(System.getProperty("user.dir") + "/" + filename));
			assertThat(pdf.text, containsString("The Free Encyclopedia"));
			// NOTE: locale UTF8
			assertThat(pdf.text, containsString("Русский"));
			assertThat(pdf.text, containsString("Français"));
			assertThat(pdf.encrypted, is(false));
			assertThat(pdf.numberOfPages, equalTo(2));

		} catch (UnsupportedEncodingException e) {
			System.err.println("Exception (ignored): " + e.toString());
		} catch (JsonSyntaxException e) {
			System.err.println("JSON Syntax exception in " + command + " (ignored): "
					+ e.toString());
		} catch (WebDriverException e) {
			System.err.println("Web Driver exception in " + command + " (ignored): "
					+ Utils.processExceptionMessage(e.getMessage()));
		} catch (IOException e) {
			System.err.println("Exception saving image (ignored): " + e.toString());
		} catch (Exception e) {
			System.err.println("Exception in " + command + "  " + e.toString());
			e.printStackTrace();
			throw (new RuntimeException(e));
		}

	}

	private void writeToFile(byte[] data, String fileName) {
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(filename);
			DataOutputStream out = new DataOutputStream(fileOutputStream);
			out.write(data);
			// appears to be blank
			out.close();
		} catch (Exception e) {
			System.err.println("Exception saving file " + e.toString());
			e.printStackTrace();
			throw (new RuntimeException(e));
		}
	}

	// add org.apache.pdfbox.text.PDFTextStripper
	// to inspect the PDF contents
	// origin: https://github.com/codeborne/pdf-test
	public static class PDF {
		public final byte[] content;

		public final String text;
		public final int numberOfPages;
		public final String author;
		public final String creator;
		public final String keywords;
		public final String producer;
		public final String subject;
		public final String title;
		public final boolean encrypted;
		public final boolean signed;
		public final String signerName;

		private PDF(String name, byte[] content) {
			this(name, content, 1, Integer.MAX_VALUE);
		}

		private PDF(String name, byte[] content, int startPage, int endPage) {
			this.content = content;

			try (InputStream inputStream = new ByteArrayInputStream(content)) {
				try (PDDocument pdf = PDDocument.load(inputStream)) {
					PDFTextStripper pdfTextStripper = new PDFTextStripper();
					pdfTextStripper.setStartPage(startPage);
					pdfTextStripper.setEndPage(endPage);
					this.text = pdfTextStripper.getText(pdf);
					this.numberOfPages = pdf.getNumberOfPages();
					this.author = pdf.getDocumentInformation().getAuthor();
					// this.creationDate = pdf.getDocumentInformation().getCreationDate();
					this.creator = pdf.getDocumentInformation().getCreator();
					this.keywords = pdf.getDocumentInformation().getKeywords();
					this.producer = pdf.getDocumentInformation().getProducer();
					this.subject = pdf.getDocumentInformation().getSubject();
					this.title = pdf.getDocumentInformation().getTitle();
					this.encrypted = pdf.isEncrypted();

					PDSignature signature = pdf.getLastSignatureDictionary();
					this.signed = signature != null;
					this.signerName = signature == null ? null : signature.getName();
				}
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid PDF file: " + name, e);
			}
		}

		public PDF(File pdfFile) throws IOException {
			this(pdfFile.getAbsolutePath(),
					Files.readAllBytes(Paths.get(pdfFile.getAbsolutePath())));
		}

		public PDF(URL url) throws IOException {
			this(url.toString(), readBytes(url));
		}

		public PDF(byte[] content) {
			this("", content);
		}

		public PDF(InputStream inputStream) throws IOException {
			this(readBytes(inputStream));
		}

		private static byte[] readBytes(URL url) throws IOException {
			try (InputStream inputStream = url.openStream()) {
				return readBytes(inputStream);
			}
		}

		private static byte[] readBytes(InputStream inputStream)
				throws IOException {
			ByteArrayOutputStream result = new ByteArrayOutputStream(2048);
			byte[] buffer = new byte[2048];

			int nRead;
			while ((nRead = inputStream.read(buffer, 0, buffer.length)) != -1) {
				result.write(buffer, 0, nRead);
			}

			return result.toByteArray();
		}
	}
}
