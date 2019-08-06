package cn.kuwo.plugin.actions;

import cn.kuwo.plugin.CommitListObservable;
import cn.kuwo.plugin.help.CommitFilter;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;

public final class StatusPopupAction extends BasePopupAction {
    private final Project project;

    //reviewed //unreview
    public static enum Status {
        All, Unreview, Reviewed
    }

    public StatusPopupAction(Project project, String filterName) {
        super(filterName);
        this.project = project;
        updateFilterValueLabel(Status.All.name());
    }

    @Override
    protected void createActions(Consumer<AnAction> actionConsumer) {
        for (Status status : Status.values()) {
            actionConsumer.consume(new DumbAwareAction(status.name()) {
                @Override
                public void actionPerformed(AnActionEvent e) {
                    updateFilterValueLabel(status.name());
                    if (status.equals(Status.All)) {
                        CommitFilter.getInstance().state = "";
                    } else {
                        CommitFilter.getInstance().state = status.name().toLowerCase();
                    }
                    CommitListObservable.getInstance().filt();
                }
            });
        }
    }
}