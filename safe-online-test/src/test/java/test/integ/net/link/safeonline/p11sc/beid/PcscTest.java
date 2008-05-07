package test.integ.net.link.safeonline.p11sc.beid;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;

import net.link.safeonline.auth.pcsc.IdentityFile;
import net.link.safeonline.auth.pcsc.Pcsc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

public class PcscTest {

	private static final Log LOG = LogFactory.getLog(PcscTest.class);
	
	@Test
	public void testReadIdentityFile() throws Exception {
		TerminalFactory terminalFactory = TerminalFactory.getDefault();
		CardTerminals cardTerminals = terminalFactory.terminals();
		CardTerminal cardTerminal = cardTerminals.list().get(0);
		Card card = cardTerminal.connect("T=0");
		CardChannel channel = card.getBasicChannel();
		Pcsc pcsc = new Pcsc(channel);
		IdentityFile identity = pcsc.getIdentityFile();
		LOG.debug("dob: " + identity.getBirthDate());
		LOG.debug("first name: " + identity.getFirstName());
	}
}