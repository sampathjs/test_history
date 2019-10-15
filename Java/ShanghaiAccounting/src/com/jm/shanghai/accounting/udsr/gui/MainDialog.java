package com.jm.shanghai.accounting.udsr.gui;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTabbedPane;

import com.jm.shanghai.accounting.udsr.RuntimeAuditingData;
import com.jm.shanghai.accounting.udsr.model.retrieval.ColumnSemantics;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationColDescription;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationColDescriptionLoader;
import com.olf.embedded.application.Display;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Transaction;

/*
 * History:
 * 2019-04-30	V1.0	jwaechter	- initial version
 * 2019-07-18	V1.1	jwaechter   - refactoring, moving mapping logics
 */

/**
 * @author jwaechter
 * @version 1.1
 */
public class MainDialog extends JFrame {
	// find below attributes to stay in this class
	private final Session session;
	private final Display display;
	private final Map<String, Table> simNameToSimResult;
	private final AccountingOperatorsGui mainThread; 
	private RuntimeAuditingData runtimeAuditingData;
	private List<Transaction> transactions=null;
	private JTabbedPane tabbedPane;
	private List<MappingTablePane> mappingTablePanes;
	private RetrievalConfigurationColDescriptionLoader colLoader;
	
	/**
	 * Indicates whether the user has finished the input and closed the dialog.
	 */
	private boolean finished=false;
	/**
	 * Contains the size of the window.
	 */
	private final Dimension size;

	/**
	 * Creates the dialog. 
	 * @param message the message shown to the user
	 * @param display the display object from the context.
	 */
	public MainDialog(final AccountingOperatorsGui mainThread, final Display display, final Session session) {
		simNameToSimResult = new HashMap<String, Table>();
		this.display = display;
		this.session = session;
		this.mainThread = mainThread;
		colLoader = new RetrievalConfigurationColDescriptionLoader(session);
		setBounds(1, 1, 1300, 768);
		size = getBounds().getSize();

		setTitle("Accounting Operators GUI");

		getRootPane().setWindowDecorationStyle(JRootPane.NONE);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        mappingTablePanes = new ArrayList<>();
        
        for (RetrievalConfigurationColDescription rccd : getRetrievalConfigurationColDescriptions()) {
        	if (rccd.getUsageType() == ColumnSemantics.MAPPER_COLUMN) {
        		MappingTablePane mtp = new MappingTablePane(this, session, rccd);
        		mappingTablePanes.add(mtp);
        		tabbedPane.add(mtp, rccd.getColTitle());
        	}
        }
  		tabbedPane.setEnabled(true);
		add(tabbedPane);
		
		Dimension frameSize = getSize();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int top = (screenSize.height - frameSize.height) / 2;
		int left = (screenSize.width - frameSize.width) / 2;
		
		setLocation(left, top);
		setVisible(true);
	}
	
    public List<RetrievalConfigurationColDescription> getRetrievalConfigurationColDescriptions() {
    	List<RetrievalConfigurationColDescription> colDescriptions = new
    			ArrayList<RetrievalConfigurationColDescription>(colLoader.getColDescriptions());
    	Collections.sort(colDescriptions, new Comparator<RetrievalConfigurationColDescription> () {
			@Override
			public int compare(RetrievalConfigurationColDescription arg0,
					RetrievalConfigurationColDescription arg1) {
				return arg0.getMappingTableEvaluationOrder() - arg1.getMappingTableEvaluationOrder();
			}
    	});
    	return colDescriptions;
	}

	protected JComponent makeTextPanel(String text) {
        JPanel panel = new JPanel(false);
        JLabel filler = new JLabel(text);
        filler.setHorizontalAlignment(JLabel.CENTER);
        panel.setLayout(new GridLayout(1, 1));
        panel.add(filler);
        return panel;
    }


	public synchronized boolean isFinished() {
		return finished;
	}

	public synchronized void setFinished(boolean finished) {
		this.finished = finished;
	}

	public Dimension getSize() {
		return size;
	}

	public Session getSession() {
		return session;
	}

	public AccountingOperatorsGui getMainThread() {
		return mainThread;
	}

	public List<Transaction> getTransactions() {
		return transactions;
	}

	public void setTransaction(List<Transaction> transactions) {
		this.transactions = transactions;
	}

	public void clearSimCache() {
		simNameToSimResult.clear();
	}


	public Display getDisplay() {
		return display;
	}


	public Map<String, Table> getSimNameToSimResult() {
		return simNameToSimResult;
	}


	public RuntimeAuditingData getRuntimeAuditingData() {
		return runtimeAuditingData;
	}


	public void setRuntimeAuditingData(RuntimeAuditingData runtimeAuditingData) {
		this.runtimeAuditingData = runtimeAuditingData;
		for (MappingTablePane mtp : mappingTablePanes) {
			mtp.notifyRuntimeAuditingTableChanged();
		}
	}

	public void notifySimResultsChanged() {
		for (MappingTablePane mtp : mappingTablePanes) {
			mtp.notifySimResultChange();
		}
	}

	public void notifySeletedSimChanged(int selectedIndex) {
		for (MappingTablePane mtp : mappingTablePanes) {
			mtp.notifySimChanged(selectedIndex);
		}		
	}

	public RetrievalConfigurationColDescriptionLoader getColLoader() {
		return colLoader;
	}

	public JTabbedPane getTabbedPane() {
		return tabbedPane;
	}
}
