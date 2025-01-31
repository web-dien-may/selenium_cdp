package com.github.sergueik.selenium;

/* Copyright 2022 Serguei Kouzmine */
import javax.imageio.ImageIO;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.devtools.v112.emulation.Emulation;
import org.openqa.selenium.devtools.v112.page.Page;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;

/**
 * Selected test scenarios for Selenium 4 Chrome Developer Tools bridge
 * https://github.com/rookieInTraining/selenium-cdp-examples/blob/main/src/test/java/com/rookieintraining/cdp/examples/Pages.java
 * https://chromedevtools.github.io/devtools-protocol/tot/Page/#method-getLayoutMetrics
 * https://chromedevtools.github.io/devtools-protocol/tot/Emulation/#method-setDeviceMetricsOverride
 * https://chromedevtools.github.io/devtools-protocol/tot/Page#method-captureScreenshot
 * https://chromedevtools.github.io/devtools-protocol/tot/Emulation/#method-clearDeviceMetricsOverride
 *
 */

public class ScreenShotCdpTest extends BaseCdpTest {
	
	private static int delay = 3000;
	private static Gson gson = new Gson();

	private static String command = null;
	private static Map<String, Object> result = new HashMap<>();
	private static Map<String, Object> params = new HashMap<>();
	private static String dataString = null;
	public static Long nodeId = (long) -1;

	private static String baseURL = "https://www.wikipedia.org";
	private double[] deviceScaleFactors = { 0.85, 0.5, 0.35, 0.25 };
	private static String filename = "temp.jpg";
	private static Base64 base64 = new Base64();
	private static Map<String, Long> rect;

	private static long width = 800;
	private static long height = 600;

	@Before
	public void beforeTest() throws Exception {
		driver.get(baseURL);
	}

	@After
	public void clearPage() {
		driver.get("about:blank");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void test() {
		// Assert
		params = new HashMap<>();
		for (int cnt = 0; cnt != deviceScaleFactors.length; cnt++) {
			double deviceScaleFactor = deviceScaleFactors[cnt];
			filename = String.format("test2_%03d.jpg",
					(int) (100 * deviceScaleFactor));

			try {
				command = "Page.getLayoutMetrics";
				result = driver.executeCdpCommand(command, new HashMap<>());
				System.err
						.println("Page.getLayoutMetrics: " + result.get("contentSize"));
				rect = (Map<String, Long>) result.get("contentSize");
				height = rect.get("height");
				width = rect.get("width");
				command = "Emulation.setDeviceMetricsOverride";
				// Act
				System.err.println(String.format("Scaling to %02d%% %s",
						(int) (100 * deviceScaleFactor), filename));
				params.clear();
				params.put("deviceScaleFactor", deviceScaleFactor);
				params.put("width", width);
				params.put("height", height);
				params.put("mobile", false);
				params.put("scale", 1);
				driver.executeCdpCommand(command, params);

				Utils.sleep(delay);
				command = "Page.captureScreenshot";
				// Act
				result = driver.executeCdpCommand(command,
						new HashMap<String, Object>());

				command = "Emulation.clearDeviceMetricsOverride";
				driver.executeCdpCommand(command, new HashMap<String, Object>());

				// Assert
				assertThat(result, notNullValue());
				assertThat(result, hasKey("data"));
				dataString = (String) result.get("data");
				assertThat(dataString, notNullValue());

				byte[] image = base64.decode(dataString);
				BufferedImage o = ImageIO.read(new ByteArrayInputStream(image));
				assertThat(o.getWidth(), greaterThan(0));
				assertThat(o.getHeight(), greaterThan(0));
				FileOutputStream fileOutputStream = new FileOutputStream(filename);
				fileOutputStream.write(image);
				fileOutputStream.close();
			} catch (IOException e) {
				System.err.println("Exception saving image (ignored): " + e.toString());
			} catch (JsonSyntaxException e) {
				System.err.println("JSON Syntax exception in " + command
						+ " (ignored): " + e.toString());
			} catch (WebDriverException e) {
				// willbe thrown if the required arguments are not provided.
				// TODO: add failing test
				System.err.println(
						"Web Driver exception in " + command + " (ignored): " + Utils
								.processExceptionMessage(e.getMessage() + "  " + e.toString()));
			} catch (Exception e) {
				System.err.println("Exception in " + command + "  " + e.toString());
				throw (new RuntimeException(e));
			}
		}
	}
}
