package cn.kuwo.plugin.view;

import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.EditSourceAction;
import com.intellij.ide.impl.TypeSafeDataProviderAdapter;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.ChangesUtil;
import com.intellij.openapi.vcs.changes.actions.OpenRepositoryVersionAction;
import com.intellij.openapi.vcs.changes.actions.RevertSelectedChangesAction;
import com.intellij.openapi.vcs.changes.actions.ShowDiffWithLocalAction;
import com.intellij.openapi.vcs.changes.committed.CommittedChangesBrowserUseCase;
import com.intellij.openapi.vcs.changes.committed.RepositoryChangesBrowser;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowser;
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class MyRepositoryChangesBrowser extends ChangesBrowser {
    public MyRepositoryChangesBrowser(Project project, List<CommittedChangeList> changeLists) {
        this(project, changeLists, Collections.emptyList(), (ChangeList)null);
    }

    public MyRepositoryChangesBrowser(Project project, List<? extends ChangeList> changeLists, List<Change> changes, ChangeList initialListSelection) {
        this(project, changeLists, changes, initialListSelection, (VirtualFile)null);
    }

    public MyRepositoryChangesBrowser(Project project, List<? extends ChangeList> changeLists, List<Change> changes, ChangeList initialListSelection, VirtualFile toSelect) {
        super(project, changeLists, changes, initialListSelection, false, false, (Runnable)null, MyUseCase.COMMITTED_CHANGES, toSelect);
    }
    private CommittedChangesBrowserUseCase myUseCase;
    private EditSourceAction myEditSourceAction;

    protected void buildToolBar(DefaultActionGroup toolBarGroup) {
        super.buildToolBar(toolBarGroup);
        toolBarGroup.add(new ShowDiffWithLocalAction());
        this.myEditSourceAction = new MyEditSourceAction();
        this.myEditSourceAction.registerCustomShortcutSet(CommonShortcuts.getEditSource(), this);
        toolBarGroup.add(this.myEditSourceAction);
        OpenRepositoryVersionAction action = new OpenRepositoryVersionAction();
        toolBarGroup.add(action);
        ActionGroup group = (ActionGroup) ActionManager.getInstance().getAction("RepositoryChangesBrowserToolbar");
        AnAction[] actions = group.getChildren((AnActionEvent)null);
        AnAction[] var6 = actions;
        int var7 = actions.length;

        for(int var8 = 0; var8 < var7; ++var8) {
            AnAction anAction = var6[var8];
            toolBarGroup.add(anAction);
        }

    }

    public void setUseCase(CommittedChangesBrowserUseCase useCase) {
        this.myUseCase = useCase;
    }

    public Object getData(@NonNls String dataId) {
        if (CommittedChangesBrowserUseCase.DATA_KEY.is(dataId)) {
            return this.myUseCase;
        } else if (VcsDataKeys.SELECTED_CHANGES.is(dataId)) {
            List<Change> list = this.myViewer.getSelectedChanges();
            return list.toArray(new Change[list.size()]);
        } else if (VcsDataKeys.CHANGE_LEAD_SELECTION.is(dataId)) {
            Change highestSelection = (Change)this.myViewer.getHighestLeadSelection();
            return highestSelection == null ? new Change[0] : new Change[]{highestSelection};
        } else if (VcsDataKeys.VCS.is(dataId)) {
            Set<AbstractVcs> abstractVcs = ChangesUtil.getAffectedVcses(this.myViewer.getSelectedChanges(), this.myProject);
            return abstractVcs.size() == 1 ? ((AbstractVcs) ObjectUtils.assertNotNull(ContainerUtil.getFirstItem(abstractVcs))).getKeyInstanceMethod() : null;
        } else {
            TypeSafeDataProviderAdapter adapter = new TypeSafeDataProviderAdapter(this);
            return adapter.getData(dataId);
        }
    }

    public EditSourceAction getEditSourceAction() {
        return this.myEditSourceAction;
    }

    private class MyEditSourceAction extends EditSourceAction {
        private final Icon myEditSourceIcon;

        public MyEditSourceAction() {
            this.myEditSourceIcon = AllIcons.Actions.EditSource;
        }

        public void update(AnActionEvent event) {
            super.update(event);
            event.getPresentation().setIcon(this.myEditSourceIcon);
            event.getPresentation().setText("Edit Source");
            if (!ModalityState.NON_MODAL.equals(ModalityState.current()) || CommittedChangesBrowserUseCase.IN_AIR.equals(CommittedChangesBrowserUseCase.DATA_KEY.getData(event.getDataContext()))) {
                event.getPresentation().setEnabled(false);
            }

        }

        protected Navigatable[] getNavigatables(DataContext dataContext) {
            Change[] changes = (Change[])VcsDataKeys.SELECTED_CHANGES.getData(dataContext);
            return changes != null ? ChangesUtil.getNavigatableArray(MyRepositoryChangesBrowser.this.myProject, ChangesUtil.getFiles(Stream.of(changes))) : null;
        }
    }
}
