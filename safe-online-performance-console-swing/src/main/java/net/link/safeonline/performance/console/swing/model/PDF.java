/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */
package net.link.safeonline.performance.console.swing.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import net.link.safeonline.performance.console.ScenarioExecution;
import net.link.safeonline.performance.console.swing.data.ConsoleAgent;
import net.link.safeonline.performance.console.swing.data.ConsoleData;
import net.link.safeonline.performance.console.swing.ui.ExecutionInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lowagie.text.Cell;
import com.lowagie.text.Chapter;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Section;
import com.lowagie.text.Table;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.tools.Executable;

/**
 * <h2>{@link PDF}<br>
 * <sub>Renders charts to a PDF file and opens it.</sub></h2>
 *
 * <p>
 * <i>Feb 19, 2008</i>
 * </p>
 *
 * @author mbillemo
 */
public class PDF {

	static final Log LOG = LogFactory.getLog(PDF.class);

	public static boolean generate() {

		// Download all charts.
		Set<ConsoleAgent> agents = ConsoleData.getSelectedAgents();
		Map<ConsoleAgent, ScenarioExecution> agentCharts = new HashMap<ConsoleAgent, ScenarioExecution>(
				agents.size());
		for (ConsoleAgent agent : agents)
			agentCharts.put(agent, agent.getStats(ConsoleData.getExecution()
					.getStartTime()));

		// Calculate total execution speed.
		double speed = 0;
		for (ScenarioExecution execution : agentCharts.values())
			if (execution.getSpeed() != null)
				speed += execution.getSpeed() * 1000d;

		// Get execution metadata from the first agent.
		ScenarioExecution execution = agentCharts.values().iterator().next();

		// Guess the real execution speed if we don't select all agents that
		// participated in the execution for PDF generation.
		// (Because we don't have the ScenarioExecution objects for those agents
		// that aren't selected but did participate).
		long duration = execution.getDuration();
		Date startTime = execution.getStartTime();
		speed *= execution.getAgents() / agentCharts.size();

		// Choose output.
		File pdfFile = chooseOutputFile(new File(String.format(
				"%s-%s-%dmin-%dx%d.pdf", new SimpleDateFormat(
						"dd.MM.yyyy-HH.mm.ss").format(startTime), execution
						.getHostname().split(":")[0], duration / 60000,
				execution.getAgents(), execution.getWorkers())));
		if (pdfFile == null)
			return false;

		try {
			// Find the max width and height of all charts.
			float width = 0, height = 0;
			for (ScenarioExecution agent : agentCharts.values())
				for (byte[][] charts : agent.getCharts().values())
					for (byte[] chart : charts) {
						ImageIcon image = new ImageIcon(chart);
						width = Math.max(width, image.getIconWidth());
						height = Math.max(height, image.getIconHeight());
					}

			// Create the PDF document.
			Document pdfDocument = new Document(new com.lowagie.text.Rectangle(
					width + 100, height + 200), 50, 50, 50, 50);
			PdfWriter.getInstance(pdfDocument, new FileOutputStream(pdfFile));
			BaseFont font = BaseFont.createFont(BaseFont.COURIER_BOLD,
					BaseFont.WINANSI, false);
			pdfDocument.open();

			// Create front page information.
			List<Cell> frontCells = new ArrayList<Cell>();
			frontCells.add(new Cell(new Phrase(
					"Safe Online:  Performance Testing", new Font(font, 40f))));
			frontCells.add(new Cell(new Phrase(50f,
					execution.getScenarioName(), new Font(font, 20f))));
			frontCells.add(new Cell(new Phrase(150f,
					String.format("Started: %s at %s",
							DateFormat.getDateInstance(DateFormat.LONG).format(
									startTime), DateFormat.getTimeInstance(
									DateFormat.LONG).format(startTime)),
					new Font(font, 20f))));
			frontCells.add(new Cell(new Phrase(50f, String.format(
					"Using %d agent%s with %d worker%s each.", execution
							.getAgents(), execution.getAgents() > 1 ? "s" : "",
					execution.getWorkers(), execution.getWorkers() > 1 ? "s"
							: ""), new Font(font, 20f))));
			frontCells.add(new Cell(new Phrase(50f, String.format(
					"OLAS Host: %s", execution.getHostname()), new Font(font,
					20f))));
			frontCells.add(new Cell(new Phrase(50f, String.format(
					"Duration: %s", ExecutionInfo.formatDuration(duration)),
					new Font(font, 20f))));
			frontCells.add(new Cell(new Phrase(100f, String.format(
					"Average Execution Speed: %.2f scenarios/s", speed),
					new Font(font, 20f))));

			// Style front page information and add it to the PDF.
			Table front = new Table(1);
			front.setBorder(0);
			front.setOffset(height / 3);
			for (Cell cell : frontCells) {
				cell.setHorizontalAlignment(Element.ALIGN_CENTER);
				cell.setBorder(0);
				front.addCell(cell);
			}
			pdfDocument.add(front);
			pdfDocument.newPage();

			// Create chapters from the labels of the first agent's charts.
			List<Chapter> chapters = new ArrayList<Chapter>();
			List<String> labels = new ArrayList<String>(agentCharts.values()
					.iterator().next().getCharts().keySet());
			for (int chapter = 0; chapter < labels.size(); ++chapter)
				chapters.add(new Chapter(new Paragraph(labels.get(chapter),
						new Font(font, 20f)), chapter + 1));

			// Arrange all the data in the chapters.
			for (int chapter = 0; chapter < chapters.size(); ++chapter) {
				boolean newPage = false;

				for (Map.Entry<ConsoleAgent, ScenarioExecution> charts : agentCharts
						.entrySet()) {
					Section section = chapters.get(chapter).addSection(
							new Paragraph(charts.getKey().getAddress()
									.toString(), new Font(font, 15f)));

					section.setTriggerNewPage(newPage);
					newPage = true;

					for (byte[] chart : new ArrayList<byte[][]>(charts
							.getValue().getCharts().values()).get(chapter))
						section.add(Image.getInstance(chart));
				}
			}

			// Add the completed chapters to the PDF.
			for (Chapter chapter : chapters) {
				pdfDocument.add(chapter);
				pdfDocument.newPage();
			}
			pdfDocument.close();

			// Open the PDF document.
			Executable.openDocument(pdfFile);
			return true;
		}

		catch (FileNotFoundException error) {
			LOG.error("File not found: " + pdfFile, error);
		} catch (DocumentException error) {
			LOG.error("Couldn't create a document writer.", error);
		} catch (IOException error) {
			LOG.error("Couldn't handle image data.", error);
		}

		return false;
	}

	/**
	 * Ask the user where to save the PDF.
	 */
	private static File chooseOutputFile(File suggestion) {

		JFileChooser pdfChooser = new JFileChooser();
		pdfChooser.addChoosableFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {

				return f.isFile() && f.canWrite()
						&& f.getName().endsWith(".pdf");
			}

			@Override
			public String getDescription() {

				return "PDF File";
			}
		});
		pdfChooser.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {

				return f.isDirectory() || f.isFile() && f.canWrite()
						&& f.getName().endsWith(".pdf");
			}

			@Override
			public String getDescription() {

				return "PDF File";
			}
		});
		pdfChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		pdfChooser.setSelectedFile(suggestion);
		if (pdfChooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
			return null;

		return pdfChooser.getSelectedFile();
	}
}
