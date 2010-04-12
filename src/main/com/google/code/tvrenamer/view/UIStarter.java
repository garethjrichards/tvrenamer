package com.google.code.tvrenamer.view;

import java.io.File;
import java.io.InputStream;
import java.text.Collator;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.swing.JOptionPane;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.google.code.tvrenamer.controller.TVRenamer;
import com.google.code.tvrenamer.model.FileEpisode;
import com.google.code.tvrenamer.model.Season;
import com.google.code.tvrenamer.model.Show;
import com.google.code.tvrenamer.model.ShowStore;
import com.google.code.tvrenamer.model.util.Constants;
import com.google.code.tvrenamer.model.util.TVRenamerLogger;
import com.google.code.tvrenamer.model.util.Constants.SWTMessageBoxType;

public class UIStarter {
  private static final String pathSeparator = System.getProperty("file.separator");
  public static final String DEFAULT_FORMAT_STRING = "%S [%sx%e] %t";

  private static TVRenamerLogger logger = new TVRenamerLogger(UIStarter.class);

  private Display display;
  private static Shell shell;

  private Button btnBrowse;
  private Button btnReset;
  private Button btnFormat;
  private Button btnRenameAll;
  private Button btnRenameSelected;

  private Table tblResults;

  private Text textFormat;

  private Label lblStatus;

  private List<FileEpisode> files;

  public static void main(String[] args) {
    UIStarter ui = new UIStarter();
    ui.init();
    ui.launch();
  }

  private void init() {
    // Set up environment
    GridLayout gridLayout = new GridLayout(5, false);
    Display.setAppName(Constants.APP_NAME);
    display = new Display();

    shell = new Shell(display);
    shell.setText(Constants.APP_NAME);
    shell.setLayout(gridLayout);

    setupBrowseDialog();
    setupFormatBox();
    setupResultsTable();
    setupTableDragDrop();
    setupMenuBar();
    setupMainWindow();

    setApplicationIcon();
  }

  private void setupMainWindow() {
    btnRenameAll = new Button(shell, SWT.PUSH);
    btnRenameAll.setText("Rename All");

    btnRenameSelected = new Button(shell, SWT.PUSH);
    btnRenameSelected.setText("Rename Selected");

    lblStatus = new Label(shell, SWT.NONE);
    lblStatus.setText("");
    lblStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    final Button btnQuit = new Button(shell, SWT.PUSH);
    btnQuit.setText("Quit");
    btnQuit.setLayoutData(new GridData(SWT.END, SWT.FILL, false, false));

    btnRenameAll.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        renameFiles(true);
      }
    });

    btnRenameSelected.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        renameFiles(false);
      }
    });

    btnQuit.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        display.dispose();
      }
    });
  }

  private void setupMenuBar() {
    Menu menuBar = new Menu(shell, SWT.BAR);
    MenuItem fileMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
    fileMenuHeader.setText("&File");

    Menu fileMenu = new Menu(shell, SWT.DROP_DOWN);
    fileMenuHeader.setMenu(fileMenu);

    MenuItem filePerferencesItem = new MenuItem(fileMenu, SWT.PUSH);
    filePerferencesItem.setText("&Perferences");

    MenuItem fileExitItem = new MenuItem(fileMenu, SWT.PUSH);
    fileExitItem.setText("E&xit");

    MenuItem helpMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
    helpMenuHeader.setText("&Help");

    Menu helpMenu = new Menu(shell, SWT.DROP_DOWN);
    helpMenuHeader.setMenu(helpMenu);

    MenuItem helpGetHelpItem = new MenuItem(helpMenu, SWT.PUSH);
    helpGetHelpItem.setText("&Get Help");

    MenuItem helpAboutItem = new MenuItem(helpMenu, SWT.PUSH);
    helpAboutItem.setText("&About");

    filePerferencesItem.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        showPreferencesPane();
      }
    });

    fileExitItem.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        shell.close();
        display.dispose();
      }

      @Override
      public void widgetDefaultSelected(SelectionEvent event) {
        shell.close();
        display.dispose();
      }
    });

    helpAboutItem.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        showAboutPane();
      }
    });

    shell.setMenuBar(menuBar);

    // fileSaveItem.addSelectionListener(new fileSaveItemListener());
    // helpGetHelpItem.addSelectionListener(new helpGetHelpItemListener());
  }

  private void setupBrowseDialog() {
    final FileDialog fd = new FileDialog(shell, SWT.MULTI);
    btnBrowse = new Button(shell, SWT.PUSH);
    btnBrowse.setText("Browse files...");

    btnBrowse.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {

        String pathPrefix = fd.open();
        if (pathPrefix != null) {
          File file = new File(pathPrefix);
          pathPrefix = file.getParent();

          String[] fileNames = fd.getFileNames();
          for (int i = 0; i < fileNames.length; i++) {
            fileNames[i] = pathPrefix + pathSeparator + fileNames[i];
          }

          initiateRenamer(fileNames);
        }
      }
    });
  }

  private void setupFormatBox() {
    final Composite formatParent = new Composite(shell, SWT.NONE);
    GridData formatData = new GridData(SWT.END, SWT.CENTER, false, false);
    formatData.horizontalSpan = 4;
    formatData.widthHint = 250;
    formatParent.setLayout(new GridLayout(4, false));
    formatParent.setLayoutData(formatData);

    final Label lblFormat = new Label(formatParent, SWT.NONE);
    lblFormat.setText("Format:");

    textFormat = new Text(formatParent, SWT.LEFT | SWT.SINGLE | SWT.BORDER);
    textFormat.setText("%S [%sx%e] %t");
    textFormat.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    btnFormat = new Button(formatParent, SWT.PUSH);
    btnFormat.setText("Apply");
    shell.setDefaultButton(btnFormat);

    btnFormat.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        populateTable();
      }
    });

    btnReset = new Button(formatParent, SWT.PUSH);
    btnReset.setText("Reset");
    btnReset.setEnabled(true);

    btnReset.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        textFormat.setText(DEFAULT_FORMAT_STRING);
        populateTable();
      }
    });
  }

  private void setupResultsTable() {
    tblResults = new Table(shell, SWT.CHECK);
    tblResults.setHeaderVisible(true);
    tblResults.setLinesVisible(true);
    GridData gridData = new GridData(GridData.FILL_BOTH);
    // gridData.widthHint = 780;
    gridData.heightHint = 200;
    gridData.horizontalSpan = 5;
    tblResults.setLayoutData(gridData);

    final TableColumn col1 = new TableColumn(tblResults, SWT.LEFT);
    col1.setText("Index");
    col1.setWidth(50);

    final TableColumn col2 = new TableColumn(tblResults, SWT.LEFT);
    col2.setText("Current Name");
    col2.setWidth(350);

    final TableColumn col3 = new TableColumn(tblResults, SWT.LEFT);
    col3.setText("Proposed Name");
    col3.setWidth(350);

    // editable table
    final TableEditor editor = new TableEditor(tblResults);
    editor.horizontalAlignment = SWT.CENTER;
    editor.grabHorizontal = true;

    col1.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        tblResults.setSortDirection(tblResults.getSortDirection() == SWT.DOWN ? SWT.UP : SWT.DOWN);
        sortTable(col1, 1);
        tblResults.setSortColumn(col1);
      }
    });

    col2.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        tblResults.setSortDirection(tblResults.getSortDirection() == SWT.DOWN ? SWT.UP : SWT.DOWN);
        sortTable(col2, 2);
        tblResults.setSortColumn(col2);
      }
    });

    col3.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        tblResults.setSortDirection(tblResults.getSortDirection() == SWT.DOWN ? SWT.UP : SWT.DOWN);
        sortTable(col3, 1);
        tblResults.setSortColumn(col3);
      }
    });

    Listener tblEditListener = new Listener() {
      public void handleEvent(Event event) {
        Rectangle clientArea = tblResults.getClientArea();
        Point pt = new Point(event.x, event.y);
        int index = tblResults.getTopIndex();
        while (index < tblResults.getItemCount()) {
          boolean visible = false;
          final TableItem item = tblResults.getItem(index);
          for (int i = 0; i < tblResults.getColumnCount(); i++) {
            Rectangle rect = item.getBounds(i);
            if (rect.contains(pt)) {
              final int column = i;
              final Text text = new Text(tblResults, SWT.NONE);
              Listener textListener = new Listener() {
                @SuppressWarnings("fallthrough")
                public void handleEvent(final Event e) {
                  switch (e.type) {
                  case SWT.FocusOut:
                    item.setText(column, text.getText());
                    text.dispose();
                    break;
                  case SWT.Traverse:
                    switch (e.detail) {
                    case SWT.TRAVERSE_RETURN:
                      item.setText(column, text.getText());
                      // fall through
                    case SWT.TRAVERSE_ESCAPE:
                      text.dispose();
                      e.doit = false;
                    }
                    break;
                  }
                }
              };
              text.addListener(SWT.FocusOut, textListener);
              text.addListener(SWT.FocusIn, textListener);
              editor.setEditor(text, item, i);
              text.setText(item.getText(i));
              text.selectAll();
              text.setFocus();
              return;
            }
            if (!visible && rect.intersects(clientArea)) {
              visible = true;
            }
          }
          if (!visible) {
            return;
          }
          index++;
        }
      }
    };
    tblResults.addListener(SWT.MouseDown, tblEditListener);
  }

  private void setupTableDragDrop() {
    DropTarget dt = new DropTarget(tblResults, DND.DROP_DEFAULT | DND.DROP_MOVE);
    dt.setTransfer(new Transfer[] { FileTransfer.getInstance() });
    dt.addDropListener(new DropTargetAdapter() {
      @Override
      public void drop(DropTargetEvent e) {
        String fileList[] = null;
        FileTransfer ft = FileTransfer.getInstance();
        if (ft.isSupportedType(e.currentDataType)) {
          fileList = (String[]) e.data;
          initiateRenamer(fileList);
        }
      }
    });
  }

  private void setApplicationIcon() {
    try {
      InputStream icon = getClass().getResourceAsStream("/icons/tvrenamer.png");
      if (icon != null) {
        shell.setImage(new Image(display, icon));
      } else {
        shell.setImage(new Image(display, "res/icons/tvrenamer.png"));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void launch() {
    Display display = null;
    try {
      // place the window in the centre of the primary monitor
      Monitor primary = Display.getCurrent().getPrimaryMonitor();
      Rectangle bounds = primary.getBounds();
      Rectangle rect = shell.getBounds();
      int x = bounds.x + (bounds.width - rect.width) / 2;
      int y = bounds.y + (bounds.height - rect.height) / 2;
      shell.setLocation(x, y);

      // Start the shell
      shell.pack();
      shell.open();

      display = shell.getDisplay();
      while (!shell.isDisposed()) {
        if (!display.readAndDispatch()) {
          display.sleep();
        }
      }
      display.dispose();
    } catch (IllegalArgumentException argumentException) {
      String message = "Drag and Drop is not currently supported on your operating system, please use the 'Browse Files' option above";
      // showMessageBox(Constants.ERROR, message);
      // display.dispose();
      System.out.println(argumentException.getMessage() + " exception: " + message);
      argumentException.printStackTrace();
      JOptionPane.showMessageDialog(null, message);
      // launch();
      System.exit(1);
    } catch (Exception exception) {
      String message = "An error occoured, please check your internet connection, java version or run from the command line to show errors";
      showMessageBox(SWTMessageBoxType.ERROR, message);
      exception.printStackTrace();
    }
  }

  private void initiateRenamer(final String[] fileNames) {
    Display.getCurrent().asyncExec(new Runnable() {
      public void run() {
        List<Thread> threads = new ArrayList<Thread>();
        files = new ArrayList<FileEpisode>();

        Set<String> showNames = new HashSet<String>();
        for (String fileName : fileNames) {
          final FileEpisode episode = TVRenamer.parseFilename(fileName);
          if (episode == null) {
            System.err.println("Couldn't parse file: " + fileName);
          } else {
            final String showName = episode.getShowName();
            if (!showNames.contains(showName)) {
              showNames.add(showName);

              Thread t = new Thread(new Runnable() {
                public void run() {
                  ShowStore.addShow(showName);
                }
              });
              threads.add(t);
              t.start();
            }
            files.add(episode);
          }
        }

        // don't really like this, would prefer to do the shows as they get
        // downloaded, but will do for now

        for (Thread t : threads) {
          try {
            t.join();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }

        // all threads should have finished by now
        populateTable();
        lblStatus.setText("");
      }
    });

    lblStatus.setText("Please wait ...");

  }

  private void renameFiles(boolean all) {
    int renamedFiles = 0;
    for (TableItem item : tblResults.getItems()) {
      if (all || item.getChecked()) {
        int index = Integer.parseInt(item.getText(0)) - 1;

        FileEpisode episode = files.get(index);
        File file = episode.getFile();
        File newFile = new File(file.getParent() + pathSeparator + item.getText(2));

        if (newFile.exists()) {
          String message = "File " + newFile + " already exists.\n" + file + " was not renamed!";
          showMessageBox(SWTMessageBoxType.QUESTION, message);
        } else {
          file.renameTo(newFile);
          logger.info("Renamed " + file.getAbsolutePath() + " to " + newFile.getAbsolutePath());
          renamedFiles++;
          episode.setFile(newFile);
        }
      }
    }

    if (renamedFiles > 0) {
      if (renamedFiles == 1) {
        lblStatus.setText(renamedFiles + " file successfully renamed.");
      } else {
        lblStatus.setText(renamedFiles + " files successfully renamed.");
      }

      lblStatus.pack(true);
      populateTable();
    }

  }

  private void populateTable() {
    if (files == null) {
      return;
    }
    // Clear the table for new use
    tblResults.removeAll();
    for (int i = 0; i < files.size(); i++) {
      FileEpisode episode = files.get(i);
      String newFilename = getNewFilename(episode);
      TableItem item = new TableItem(tblResults, SWT.NONE);
      item.setText(new String[] { String.valueOf(i + 1), episode.getFile().getName(), newFilename });
      item.setChecked(true);
    }
  }

  private String getNewFilename(FileEpisode episode) {
    String showName = "Show not found";
    String seasonNum = "Season not found";
    String titleString = "Episode not found";

    Show show = ShowStore.getShow(episode.getShowName().toLowerCase());
    if (show != null) {
      showName = show.getName();

      Season season = show.getSeason(episode.getSeasonNumber());
      if (season != null) {
        seasonNum = String.valueOf(season.getNumber());

        String title = season.getTitle(episode.getEpisodeNumber());
        if (title != null) {
          titleString = TVRenamer.sanitiseTitle(title);
        } else {
          titleString += " (" + episode.getEpisodeNumber() + ")";
        }
      } else {
        seasonNum += " (" + episode.getSeasonNumber() + ")";
      }
    }
    String newFilename = textFormat.getText();
    newFilename = newFilename.replaceAll("%S", showName);
    newFilename = newFilename.replaceAll("%s", seasonNum);
    newFilename = newFilename.replaceAll("%e", new DecimalFormat("00").format(episode.getEpisodeNumber()));
    newFilename = newFilename.replaceAll("%t", titleString);

    return newFilename + "." + TVRenamer.getExtension(episode.getFile().getName());
  }

  private void setSortedItem(int i, int j) {
    TableItem oldItem = tblResults.getItem(i);
    boolean wasChecked = oldItem.getChecked();
    int oldStyle = oldItem.getStyle();
    String[] values = { oldItem.getText(0), oldItem.getText(1), oldItem.getText(2) };
    oldItem.dispose();
    TableItem item = new TableItem(tblResults, oldStyle, j);
    item.setText(values);
    item.setChecked(wasChecked);

  }

  private void sortTable(TableColumn col, int position) {
    // Get the items
    TableItem[] items = tblResults.getItems();
    Collator collator = Collator.getInstance(Locale.getDefault());

    // Go through the item list and
    for (int i = 1; i < items.length; i++) {
      String value1 = items[i].getText(position);
      for (int j = 0; j < i; j++) {
        String value2 = items[j].getText(position);
        // Compare the two values and order accordingly
        if (tblResults.getSortDirection() == SWT.DOWN) {
          if (collator.compare(value1, value2) < 0) {
            setSortedItem(i, j);
            // the snippet replaces the items with the new items, we
            // do the same
            items = tblResults.getItems();
            break;
          }
        } else {
          if (collator.compare(value1, value2) > 0) {
            setSortedItem(i, j);
            // the snippet replaces the items with the new items, we
            // do the same
            items = tblResults.getItems();
            break;
          }
        }
      }
    }
  }

  private void showPreferencesPane() {
    MessageBox msgSuccess = new MessageBox(shell, SWT.OK);
    msgSuccess.setMessage("Preferences Pane placeholder");
    msgSuccess.open();
  }

  private void showAboutPane() {
    MessageBox msgSuccess = new MessageBox(shell, SWT.OK);
    msgSuccess.setMessage("TVRenamer is a Java GUI utility to rename tv episodes from tv listings");
    msgSuccess.open();
  }

  public static void showMessageBox(SWTMessageBoxType type, String message) {
    int swtIconValue = -1;

    switch (type) {
    case QUESTION:
      swtIconValue = SWT.ICON_QUESTION;
      break;
    case MESSAGE:
      swtIconValue = SWT.ICON_INFORMATION;
      break;
    case WARNING:
      swtIconValue = SWT.ICON_WARNING;
      break;
    case ERROR:
      swtIconValue = SWT.ICON_ERROR;
      break;
    case OK:
      // Intentional missing break
    default:
      swtIconValue = SWT.OK;
    }

    logger.info("swtIconValue: " + swtIconValue);
    MessageBox msgSuccess = new MessageBox(shell, swtIconValue);
    msgSuccess.setMessage(message);
    msgSuccess.open();
  }
}