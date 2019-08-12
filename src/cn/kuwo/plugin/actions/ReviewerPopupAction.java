package cn.kuwo.plugin.actions;

import cn.kuwo.plugin.CommenUtil;
import cn.kuwo.plugin.CommitListObservable;
import cn.kuwo.plugin.help.CommitFilter;
import com.google.gson.Gson;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.util.Consumer;

import javax.swing.*;
import java.util.ArrayList;

public final class ReviewerPopupAction extends BasePopupAction {
    private ArrayList<String> users;
    private Gson gson;

    public ReviewerPopupAction(Project project, String filterName) {
        super(filterName);
        updateFilterValueLabel("All");
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
        String userListJson = propertiesComponent.getValue(CommenUtil.USERLIST_JSON);
        if (userListJson != null && !userListJson.isEmpty()) {
            if (gson == null) {
                gson = new Gson();
            }
            users = gson.fromJson(userListJson, ArrayList.class);
        }
    }

    @Override
    protected void createActions(Consumer<AnAction> actionConsumer) {
        actionConsumer.consume(new DumbAwareAction("All") {
            @Override
            public void actionPerformed(AnActionEvent e) {
                updateFilterValueLabel("All");
                CommitFilter.getInstance().reviewer = "";
                CommitListObservable.getInstance(e.getProject()).filt();
            }
        });
        selectUserTextArea = new JTextArea();
        selectOkAction = buildOkAction();
        if (users != null) {
            for (String user : users) {
                actionConsumer.consume(new DumbAwareAction(user) {
                    @Override
                    public void actionPerformed(AnActionEvent e) {
                        updateFilterValueLabel(user);
                        CommitFilter.getInstance().reviewer = user;
                        CommitListObservable.getInstance(e.getProject()).filt();
                    }
                });
            }
        }
        addSelectItem(actionConsumer);
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
                    CommitFilter.getInstance().reviewer = newText;
                    CommitListObservable.getInstance(e.getProject()).filt();
                    if (users == null) {
                        users = new ArrayList<>();
                    }
                    if (!users.contains(newText)) {
                        users.add(newText);
                    }
                    if (gson == null) {
                        gson = new Gson();
                    }
                    PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(e.getProject());
                    propertiesComponent.setValue(CommenUtil.USERLIST_JSON, gson.toJson(users));
                }
            }
        };
    }
}