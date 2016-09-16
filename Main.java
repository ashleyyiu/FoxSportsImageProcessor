package com.fox.apps.soccerlogocreator;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.SystemUtils;

public class Main {

	// code sample:
	// http://www.programcreek.com/java-api-examples/index.php?api=org.im4java.core.ConvertCmd

	private String stadiumSize = "960x540!";
	private String stadiumSize2 = "416x259!";
	private String logoSize = "x320";
	private String logoSize2 = "x150";

	private int logoOffsetY = 50;
	private String inputFolderPath;
	private String tmpFolder = "tmp";
	private String outFolder = "out";
	private String convertCmd;
	private String tmpHome = "_tmpHome.png";
	private String tmpAway = "_tmpAway.png";
	private String tmpStadium = "_tmpStadium.png";
	private String tmpHomeStadium = "_tmpHomeStadium.png";
	private String finishedName = "";

	private Iterator inputIterate;
	private List<String> listClub;
	private List<String> listStadium;
	private List<Match> listMatches;
	private String backgroundImage;

	public static void main(String[] args) {
		Main m = new Main();
		m.initialize();
		
		m.generate(m.stadiumSize, m.logoSize, true);
		m.generate(m.stadiumSize, m.logoSize, false);
		m.generate(m.stadiumSize2, m.logoSize2, false);
		
		//m.generatePreSelectedMatches();
		
		m.clear();
	}

	public Main() {
		listClub = new ArrayList<String>();
		listStadium = new ArrayList<String>();
		listMatches = new ArrayList<Match>();
		
		try {
			File tmpFile = new File(tmpFolder);
			if (tmpFile.exists()) {
				FileUtils.deleteDirectory(tmpFile);
			}
			tmpFile.mkdir();

			File outFile = new File(outFolder);
			if (outFile.exists()) {
				FileUtils.deleteDirectory(outFile);
			}
			outFile.mkdir();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void clear() {
		File tmpFile = new File(tmpFolder);
		if (tmpFile.exists()) {
			try {
				FileUtils.deleteDirectory(tmpFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void initialize() {
		Properties prop = new Properties();
		InputStream input = null;

		try {
			input = new FileInputStream("config.properties");
			prop.load(input);

			inputFolderPath = prop.getProperty("input_folder");
			convertCmd = prop.getProperty("convert_path");
			if (!Files.isDirectory(Paths.get(inputFolderPath))) {
				print("Directory does not exist. " + inputFolderPath);
				System.exit(1);
			}

			backgroundImage = prop.getProperty("background_image_path");
			File backgroundFile = new File(backgroundImage);
			if (!backgroundFile.exists()) {
				print("Background image does not exist. " + backgroundImage);
				System.exit(1);
			}

			// Read input folder -> list
			String[] extensions = new String[] { "jpg", "jpeg", "png", "gif" };
			IOFileFilter filter = new SuffixFileFilter(extensions,
					IOCase.INSENSITIVE);
			inputIterate = FileUtils.iterateFiles(new File(inputFolderPath),
					filter, DirectoryFileFilter.DIRECTORY);

			if (inputIterate != null && inputIterate.hasNext()) {
				while (inputIterate.hasNext()) {
					File f = (File) inputIterate.next();
					String path = f.getAbsolutePath();
					if (path.contains("_back")) {
						listStadium.add(path);
					} else {
						if (path.equalsIgnoreCase(backgroundImage))
							continue;
						listClub.add(path);
					}
				}
			}

			
			File matchFile = new File("match.properties");
			if(matchFile.exists()){
				List<String> lines= FileUtils.readLines(matchFile);
				for(String s : lines){
					try{
						String[] match = s.split(Pattern.quote("|"));
						if(match!=null && match.length>0){
							String homePath = inputFolderPath + getSlash() + prop.getProperty(match[0]);
							String awayPath = inputFolderPath + getSlash() + prop.getProperty(match[1]);
							Match m = new Match(homePath, awayPath);
							listMatches.add(m);
						}
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}
			}	
			
			
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				input.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private String getSlash() {
		if (SystemUtils.IS_OS_WINDOWS) {
			return "\\\\";
		}

		return "/";
	}

	private void generatePreSelectedMatches(){
		if(listMatches.size()>0){
			for(Match m : listMatches){
				generateV2(stadiumSize, logoSize, m.getHome(), m.getAway(), true);
				generateV2(stadiumSize, logoSize, m.getHome(), m.getAway(), false);
				generateV2(stadiumSize2, logoSize2, m.getHome(), m.getAway(), false);
			}
		}
	}
	
	private void generateV2(String stadiumSize, String logoSize, String clubHomePath, String clubAwayPath, boolean isOffsetY){
		
		//hold the resized image
		BufferedImage imgStadium;
		BufferedImage imgHomeLogo;
		BufferedImage imgAwayLogo;
		
		if (backgroundImage != null && backgroundImage.length() > 0) {
			String[] cmdArrayStadium = { convertCmd, backgroundImage,
					"-resize", stadiumSize, tmpFolder + getSlash() + tmpStadium };
			try {
				run(cmdArrayStadium);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		
		String[] cmdArrayRemoveTransparencyHome = { convertCmd, clubHomePath,
				"-trim", "+repage", tmpFolder + getSlash() + tmpHome };

		try {
			run(cmdArrayRemoveTransparencyHome);

			String[] cmdArrayHome = { convertCmd,
					tmpFolder + getSlash() + tmpHome, "-resize", logoSize,
					tmpFolder + getSlash() + tmpHome };
			run(cmdArrayHome);
			
			// get the image size and centered position in horizontal
			// and vertical
			imgStadium = ImageIO.read(new File(tmpFolder
					+ getSlash() + tmpStadium));
			imgHomeLogo = ImageIO.read(new File(tmpFolder
					+ getSlash() + tmpHome));
			

			// add shadow
			String[] cmdArrayHomeShadow = { convertCmd,
					tmpFolder + getSlash() + tmpHome, "(", "+clone",
					"-background", "black", "-shadow", "80x3+10+10", ")",
					"+swap", "-background", "none", "-layers", "merge",
					"+repage", tmpFolder + getSlash() + tmpHome };
			run(cmdArrayHomeShadow);

			String[] cmdArrayRemoveTransparencyAway = { convertCmd,
					clubAwayPath, "-trim", "+repage",
					tmpFolder + getSlash() + tmpAway };

			run(cmdArrayRemoveTransparencyAway);

			String[] cmdArrayAway = { convertCmd,
					tmpFolder + getSlash() + tmpAway, "-resize",
					logoSize, tmpFolder + getSlash() + tmpAway };

			run(cmdArrayAway);
			
			imgAwayLogo = ImageIO.read(new File(tmpFolder
					+ getSlash() + tmpAway));

			
			String[] cmdArrayAwayShadow = { convertCmd,
					tmpFolder + getSlash() + tmpAway, "(", "+clone",
					"-background", "black", "-shadow", "80x3+10+10",
					")", "+swap", "-background", "none", "-layers",
					"merge", "+repage",
					tmpFolder + getSlash() + tmpAway };
			run(cmdArrayAwayShadow);


			float startHomeX = 0;
			float startAwayX = 0;
			
			float newSpace = Math
					.abs(imgStadium.getWidth() - imgHomeLogo
							.getWidth() - imgAwayLogo.getWidth()) / 3;
			
			startHomeX = newSpace;
			startAwayX = startHomeX + imgHomeLogo.getWidth()
					+ newSpace;

			float startHomeY = 0;
			float startAwayY = 0;
			if (isOffsetY) {
				startHomeY = Math
						.abs((imgStadium.getHeight() - imgHomeLogo
								.getHeight()))
						/ 2 - logoOffsetY;
				startAwayY = Math
						.abs((imgStadium.getHeight() - imgAwayLogo
								.getHeight()))
						/ 2 - logoOffsetY;
			} else {
				startHomeY = Math
						.abs((imgStadium.getHeight() - imgHomeLogo
								.getHeight())) / 2;
				startAwayY = Math
						.abs((imgStadium.getHeight() - imgAwayLogo
								.getHeight())) / 2;
			}

			String homeLogoPosition = String.format("+%f+%f",
					startHomeX, startHomeY);
			String awayLogoPosition = String.format("+%f+%f",
					startAwayX, startAwayY);

			// combine stadium + home
			String[] cmdComb1 = { convertCmd,
					tmpFolder + getSlash() + tmpStadium,
					tmpFolder + getSlash() + tmpHome, "-geometry",
					homeLogoPosition, "-composite",
					tmpFolder + getSlash() + tmpHomeStadium };
			run(cmdComb1);

			if (isOffsetY) {
				finishedName = String.format("%s_vs_%s_%s_shifted.png",
						getClubName(clubHomePath), getClubName(clubAwayPath),
						stadiumSize.substring(0,
								stadiumSize.length() - 1));
			} else {
				finishedName = String.format("%s_vs_%s_%s.png",
						getClubName(clubHomePath), getClubName(clubAwayPath),
						stadiumSize.substring(0,
								stadiumSize.length() - 1));
			}

			// combine stadium + home + away
			String[] cmdComb2 = { convertCmd,
					tmpFolder + getSlash() + tmpHomeStadium,
					tmpFolder + getSlash() + tmpAway, "-geometry",
					awayLogoPosition, "-composite",
					outFolder + getSlash() + finishedName };
			run(cmdComb2);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	private void generate(String stadiumSize, String logoSize, boolean isOffsetY) {
		// Convert home logo -> tmpHome
		for (String clubHome : listClub) {
			for (String clubAway : listClub) {
				if (clubAway.equals(clubHome))
					continue;
				generateV2(stadiumSize, logoSize, clubHome, clubAway, isOffsetY);
				
			}
		}
	}

	private String getClubName(String str) {
		StringBuffer sb = new StringBuffer();
		String[] arrStr = str.split("\\.");
		if (arrStr.length > 0) {
			String first = arrStr[0];
			String[] newArr = first.split(getSlash());
			if (newArr.length > 0)
				sb.append(newArr[newArr.length - 1]);
		} else {
			arrStr = str.split(getSlash());
			if (arrStr.length > 0) {
				sb.append(arrStr[arrStr.length - 1]);
			}
		}
		return sb.toString();
	}

	private void print(String str) {
		System.out.println(str);
	}

	private void run(String str) throws IOException, InterruptedException {
		print(str);
		Process p = Runtime.getRuntime().exec(str);
		p.waitFor();
	}

	private void run(String[] str) throws IOException, InterruptedException {
		Process p = Runtime.getRuntime().exec(str);
		p.waitFor();
	}

	
	private class Match{
		private String home;
		private String away;
		
		public String getHome() {
			return home;
		}
		public void setHome(String home) {
			this.home = home;
		}
		public String getAway() {
			return away;
		}
		public void setAway(String away) {
			this.away = away;
		}
		public Match(){
			
		}
		public Match(String home, String away){
			this.home = home;
			this.away = away;
		}
	}
}
