package org.example;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.LocalTime;

import javax.sound.sampled.LineUnavailableException;

import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

//TODO: Check if validate is needed
// if yes, adjust views

public class OpenseaSeleniumMonitorTask extends Thread {

	static String collectionUrl = "";
	static String latestScrape = "";
	static String webhookUrl;
	int taskId;
	static String strivingProfit = "";
	static ChromeDriver page;

	public OpenseaSeleniumMonitorTask(String collectionUrl, int taskId, String strivingProfit, String webhookUrl) {
		this.collectionUrl = collectionUrl;
		this.strivingProfit = strivingProfit;
		this.taskId = taskId;
		this.webhookUrl = webhookUrl;

	}

	public void run() {
		System.out.println("Thread[" + Thread.currentThread().getName() + "] - Initializing...");

		// START BROWSER
		Playwright playwright = Playwright.create();

		Browser browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(false)

		);

		BrowserContext context = browser.newContext();
		context.setDefaultTimeout(30000);

		Page page = context.newPage();

		while (true) {
			try {
				getPageAndMonitor(page, collectionUrl, Double.valueOf(strivingProfit));

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	static int counter = 0;

	public static void superClick(ChromeDriver driver, String xpath) throws Exception {
		try {
			driver.findElement(By.xpath(xpath)).click();

		} catch (Exception e) {
			counter++;
			if (counter > 30) {
				counter = 0;
				throw new Exception("Buy now button not working");
			}

			System.out.println("Retrying...");
			e.printStackTrace();
			superClick(driver, xpath);
		}
	}

	public static void getPageAndMonitor(Page page, String collectionUrl, double strivingProfit) throws Exception {

		String pageContent = "";
		String assetUrl = "https://opensea.io/assets/ADDRESS_HERE/TOKENID_HERE";
		String imageUrl = "soon";
		String name = "";
		String address = "";
		String tokenID = "";
		String price = "";
		int maxViews = 6;
		double floor = 0;
		String[] collectionArray = collectionUrl.split("/");
		String collectionName = collectionArray[collectionArray.length - 1];

		String urlParameters = "?search[priceFilter][symbol]=ETH&[sortAscending]=true&search[sortBy]=PRICE&search[toggles][0]=BUY_NOW";
		String scrapingUrl = collectionUrl + urlParameters;

		page.navigate(scrapingUrl);

		pageContent = page.content();
		String floorString = getFloor(pageContent);
		if (floorString.equals("---")) {
			getPageAndMonitor(page, collectionUrl, strivingProfit);
		}
		floor = Double.valueOf(floorString);
		// calculate final price
		double monitoringPrice = floor - strivingProfit;
		double profit = Math.round((floor - monitoringPrice) * 1000.0) / 1000.0;

		// Look up price
		page.waitForSelector("//*[@placeholder=\"Max\"]");
		page.fill("//*[@placeholder=\"Max\"]", String.valueOf(monitoringPrice));
		Thread.sleep(200);
		page.click("//button[text()=\"Apply\"]");
		Thread.sleep(3000);

		if (page.content().contains("No items to display")) {
			System.out.println("Thread[ " + "Floor: " + floor + " | Monitoring Price: " + monitoringPrice
					+ " ] - Waiting for entries...");
			getPageAndMonitor(page, collectionUrl, strivingProfit);
		}

		FileWriter writer = new FileWriter(new File("xy.txt"));
		writer.write(page.content());
		writer.close();

		tokenID = getLatestEntry(pageContent);
		address = getAddress(pageContent);
		price = getPrice(pageContent).replace(",", ".");
		assetUrl = assetUrl.replace("ADDRESS_HERE", address).replace("TOKENID_HERE", tokenID);

		try {
			imageUrl = getImageUrl(page.content());

		} catch (Exception e) {
			imageUrl = " ";
		}
		name = getLatestEntryName(pageContent);

		System.out.println("Thread[ " + "Floor: " + floor + " | Monitoring Price: " + monitoringPrice
				+ " ] - Found entry ~ Token: #" + tokenID + " - Price: " + price + "ETH");

		System.out.println("Thread[ " + "Floor: " + floor + " | Monitoring Price: " + monitoringPrice
				+ " ] - Validating entry... ");

//		if (validateNFT(driver.getPageSource(), monitoringPrice, maxViews, String.valueOf(floor),
//				String.valueOf(strivingProfit)) == false) {
//
//			getPageAndMonitor(driver, collectionUrl, monitoringPrice);
//		}

		System.out.println(assetUrl);
		System.out.println(imageUrl);
		System.out.println(String.valueOf(floor));
		System.out.println(monitoringPrice);
		System.out.println(String.valueOf(profit));

		if (!latestScrape.equals(assetUrl) && Double.valueOf(price) <= monitoringPrice) {
			sendWebhook(collectionName, assetUrl, imageUrl, String.valueOf(floor), price, String.valueOf(profit),
					webhookUrl);
			latestScrape = assetUrl;
		} else {
			System.out.println("Skipping... already sent!");
		}

	}

	public static String getFloor(String page) throws IOException {
		String floor = "";

		Writer fileWriter = new FileWriter("content.txt");

		fileWriter.write(page);
		fileWriter.close();

		String[] floorPre = page.split(">floor price</div");
		String floor2 = floorPre[0];
		String[] floorPre3 = floor2.split("<h3");

		String floorPre4 = floorPre3[floorPre3.length - 1];

		String[] floorPre5 = floorPre4.split(">");

		String floorPre6 = floorPre5[1];

		String[] floorPre7 = floorPre4.split("</div>");

		String floorPre8 = floorPre7[0];

		String[] floorPre9 = floorPre8.split(">");

		String floorPre10 = floorPre9[floorPre9.length - 1];

		floor = floorPre10;

		return floor.strip();
	}

	public static String getAddress(String page) throws IOException {
		String address = "";

		String[] addressPre = page.split("\"address\":\"");
		String address2 = addressPre[1];
		String[] address3 = address2.split("\",");

		address = address3[0];

		return address;
	}

	public static String getLatestEntry(String page) throws IOException {
		String entry = "";

		// wenn keine Zahl
		String[] entry1 = page.split("Asset--anchor\" href=\"/assets/");
		String entry2 = entry1[1];
		String[] entry3 = entry2.split("\">");
		String entry4 = entry3[0];
		String[] entry5 = entry4.split("/");

		entry = entry5[entry5.length - 1];

		return entry;
	}

	public static String getLatestEntryName(String page) throws IOException {
		String entry = "";

		// wenn keine Zahl
		String[] entry1 = page.split("class=\"AssetCardFooter--name\">");
		String entry2 = entry1[1];
		String[] entry3 = entry2.split("<");
		String entry4 = entry3[0];

		entry = entry4;

		return entry;
	}

	public static String getPrice(String page) throws IOException {
		String price = "";

		String[] entryPre = page.split("Price--amount\">");
		String entry2 = entryPre[1];
		String[] entry3 = entry2.split(" <");

		price = entry3[0];

		return price;
	}

	public static String getImageUrl(String page) throws IOException {
		String imageUrl = "";

		String[] entryPre = page.split("gridcell");
		String entry2 = entryPre[1];

		if (entry2.contains("Image--image\" src=\""))
			System.out.println("Yes");
		else
			System.out.println(entry2);

		String[] entry3 = entry2.split("Image--image\" src=\"");
		String entry4 = entry3[1];
		String[] entry5 = entry4.split("\" ");

		imageUrl = entry5[0];

		return imageUrl;
	}

	public static String getViews(String page) throws IOException {
		String views = "";

		String[] entryPre = page.split("views</div>");
		String entry2 = entryPre[0];
		String[] entry3 = entry2.split("</div>");
		String entry4 = entry3[entry3.length - 1];

		views = entry4.strip();

		return views;
	}

	public static boolean validateNFT(String page, double priceToBeConfirmed, int maxViews, String floor, String profit)
			throws IOException {

		maxViews = 300;

		FileWriter writer = new FileWriter(new File("test.txt"));
		writer.write(page);
		writer.close();

		// validate views
		String[] viewsPre = page.split("material-icons\">visibility</i></div>"); // CHECK ID
		String viewsPre1 = viewsPre[1];
		String[] viewsPre2 = viewsPre1.split("views");
		String viewsPre3 = viewsPre2[0];

		String views = viewsPre3.strip();

		// Validate Price
		String price = "";
		String[] entryPre = page.split("TradeStation--main");
		String entry2 = entryPre[1];
		String[] entry3 = entry2.split("<span class=\"Price--raw-symbol");
		String entry4 = entry3[0];
		String[] entry5 = entry4.split("Price--amount\">");
		String entry6 = entry5[1];

		price = entry6.strip();

		if (price.contains(","))
			price = price.replace(",", ".");

		if (Double.valueOf(price) <= priceToBeConfirmed && maxViews >= Integer.valueOf(views))
			return true;
		else {
			if (!(Double.valueOf(price) <= priceToBeConfirmed))
				System.out.println("Thread[ " + "Floor: " + floor + " | Profit: " + strivingProfit
						+ " ] - Skipping entry due to high price: " + price);
			if (!(maxViews >= Integer.valueOf(views)))
				System.out.println("Thread[ " + "Floor: " + floor + " | Profit: " + strivingProfit
						+ " ] - Skipping entry due to views count: " + views);

			return false;

		}
	}

	public static void sendWebhook(String collectionName, String assetUrl, String imageUrl, String floor, String price,
			String profit, String webhookUrl) throws IOException, LineUnavailableException {

		if (imageUrl.equals(" "))
			imageUrl = "https://media.discordapp.net/attachments/889250795017617473/892727372783190016/logo-01.png";

		String[] tokenIDArray = assetUrl.split("/");
		String tokenID = tokenIDArray[tokenIDArray.length - 1];

		LocalTime now = LocalTime.now();
		DiscordWebhook webhook = new DiscordWebhook(webhookUrl);
//	    webhook.setContent("Any message!");
		webhook.setAvatarUrl(
				"https://media.discordapp.net/attachments/889250795017617473/892727372783190016/logo-01.png");
		webhook.setUsername("FLOOR MONITOR");
		webhook.setTts(true);
		webhook.addEmbed(new DiscordWebhook.EmbedObject().setTitle(collectionName.toUpperCase() + " #" + tokenID + "")
				.setDescription("").setColor(Color.BLUE).addField("Floor", floor + " Ξ", false)
				.addField("Price", price + " Ξ", false).addField("Profit", profit + " Ξ", false)
				.addField("Rank", "123", false).setThumbnail(imageUrl)
				.addField("Collection", "[**Here**](" + collectionUrl + ")", false).setThumbnail(imageUrl)
				.setFooter(now.toString().substring(0, 8) + " | Custom made by @contractrs",
						"https://media.discordapp.net/attachments/889250795017617473/892727372783190016/logo-01.png")
//	    .setImage("https://kryptongta.com/images/kryptontitle2.png")
//	    .setAuthor("Author Name", "https://kryptongta.com", "https://kryptongta.com/images/kryptonlogowide.png")
				.setUrl(assetUrl));
//	    webhook.addEmbed(new DiscordWebhook.EmbedObject()
//	    .setDescription("Just another added embed object!"));
		webhook.execute(); // Handle exception
	}
}
