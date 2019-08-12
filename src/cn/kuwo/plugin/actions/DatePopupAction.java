package cn.kuwo.plugin.actions;

import cn.kuwo.plugin.CommitListObservable;
import cn.kuwo.plugin.help.CommitFilter;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vcs.versionBrowser.DateFilterComponent;
import com.intellij.util.Consumer;
import com.intellij.util.text.DateFormatUtil;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class DatePopupAction extends BasePopupAction {
    public static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public DatePopupAction(Project project, String filterName) {
        super(filterName);
        updateFilterValueLabel("All");
    }

    @Override
    protected void createActions(Consumer<AnAction> actionConsumer) {
        actionConsumer.consume(new DumbAwareAction("All") {
            @Override
            public void actionPerformed(AnActionEvent e) {
                updateFilterValueLabel("All");
                CommitFilter.getInstance().commitStartTime = "";
                CommitFilter.getInstance().commitEndTime = "";
                CommitListObservable.getInstance(e.getProject()).filt();
            }
        });
        selectUserTextArea = new JTextArea();
        selectOkAction = buildOkAction();
        addSelectItem(actionConsumer);
    }

    @Override
    protected void addSelectItem(Consumer<AnAction> actionConsumer) {
        actionConsumer.consume(new DumbAwareAction("Today") {
            @Override
            public void actionPerformed(AnActionEvent e) {
                Date date = new Date(System.currentTimeMillis());
                String format = simpleDateFormat.format(date);

                updateFilterValueLabel("Since " + format);
                CommitFilter.getInstance().commitStartTime = format.substring(2);
                CommitFilter.getInstance().commitEndTime = "";
                CommitListObservable.getInstance(e.getProject()).filt();
            }
        });
        actionConsumer.consume(new DumbAwareAction("Last 3 days") {
            @Override
            public void actionPerformed(AnActionEvent e) {
                Date date = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000 * 3);
                String format = simpleDateFormat.format(date);
                updateFilterValueLabel("Since " + format);
                CommitFilter.getInstance().commitStartTime = format.substring(2);
                CommitFilter.getInstance().commitEndTime = "";
                CommitListObservable.getInstance(e.getProject()).filt();
            }
        });
        actionConsumer.consume(new DumbAwareAction("Last 7 days") {
            @Override
            public void actionPerformed(AnActionEvent e) {
                Date date = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000 * 7);
                String format = simpleDateFormat.format(date);
                updateFilterValueLabel("Since " + format);
                CommitFilter.getInstance().commitStartTime = format.substring(2);
                CommitFilter.getInstance().commitEndTime = "";
                CommitListObservable.getInstance(e.getProject()).filt();
            }
        });
        actionConsumer.consume(new DumbAwareAction("Select...") {

            @Override
            public void actionPerformed(AnActionEvent e) {
                final DateFilterComponent dateComponent = new DateFilterComponent(false, DateFormatUtil.getDateFormat().getDelegate());
                dateComponent.setBefore(System.currentTimeMillis());
                dateComponent.setAfter(System.currentTimeMillis());
                DialogBuilder db = new DialogBuilder(e.getProject());
                db.addOkAction();
                db.setCenterPanel(dateComponent.getPanel());
                db.setPreferredFocusComponent(dateComponent.getPanel());
                db.setTitle("Select Period");
                if (DialogWrapper.OK_EXIT_CODE == db.show()) {
                    long after = dateComponent.getAfter();
                    long before = dateComponent.getBefore();
                    if (after >= before) {
                        Messages.showMessageDialog("The start time must be before the end time.", "Error", AllIcons.Ide.Warning_notifications);
                        return;
                    }
                    String dateAfterStr = null;
                    try {
                        Date dateAfter = new Date(after);
                        dateAfterStr = simpleDateFormat.format(dateAfter);
                        CommitFilter.getInstance().commitStartTime = dateAfterStr.substring(2);
                    } catch (NullPointerException e1) {
                        CommitFilter.getInstance().commitStartTime = "";
                    }
                    String dateBeforeStr = null;
                    try {
                        Date dateBefore = new Date(before);
                        dateBeforeStr = simpleDateFormat.format(dateBefore);
                        CommitFilter.getInstance().commitEndTime = dateBeforeStr.substring(2);
                    } catch (NullPointerException e1) {
                        CommitFilter.getInstance().commitEndTime = "";
                    }
                    String dateFilte = new String();
                    if (dateAfterStr != null) {
                        dateFilte += dateAfterStr;
                    }
                    dateFilte += "/";
                    if (dateBeforeStr != null) {
                        dateFilte += dateBeforeStr;
                    }
                    updateFilterValueLabel(dateFilte);
                    CommitListObservable.getInstance(e.getProject()).filt();
                }
            }
        });
    }

    protected AnAction buildOkAction() {
        return new AnAction() {
            public void actionPerformed(AnActionEvent e) {
                popup.closeOk(e.getInputEvent());
                String newText = selectUserTextArea.getText().trim();
                if (newText.isEmpty()) {
                    return;
                }
                if (!Comparing.equal(newText, getFilterValueLabel().getText())) {
                    updateFilterValueLabel(newText);
                    CommitFilter.getInstance().user = newText;
                    CommitListObservable.getInstance(e.getProject()).filt();
                }
            }
        };
    }
}