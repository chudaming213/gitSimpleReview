package cn.kuwo.plugin.ui;

import cn.kuwo.plugin.CommitListObservable;
import cn.kuwo.plugin.actions.*;
import cn.kuwo.plugin.bean.CommitInfo;
import cn.kuwo.plugin.help.CommitFilter;
import cn.kuwo.plugin.view.CommitPanel;
import cn.kuwo.plugin.view.MyRepositoryChangesBrowser;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionManagerImpl;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserBase;
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentProvider;
import com.intellij.openapi.vcs.ui.SearchFieldAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBSplitter;
import com.intellij.util.Consumer;
import com.intellij.util.NotNullFunction;
import com.intellij.vcs.log.VcsFullCommitDetails;
import com.intellij.vcs.log.VcsLogProvider;
import com.intellij.vcs.log.data.VcsLogData;
import com.intellij.vcs.log.impl.VcsLogManager;
import com.intellij.vcs.log.impl.VcsLogTabsProperties;
import com.intellij.vcs.log.ui.VcsLogColorManagerImpl;
import git4idea.GitVcs;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class ReviewContent implements ChangesViewContentProvider {

    private Observer listDataObserver;
    private Project project;
    private JPanel panel1;
    private JList reviewList;
    private DataModel dataModel;
    private JBSplitter horizontalSplitter;
    private ChangesBrowserBase changesBrowserBase;
    private JBSplitter verticalSplitter;
    private CommitPanel commitPanel;
    private AcceptAction acceptAction;
    private CommentAction commentAction;
    private VcsLogData vcsLogData;

    public ReviewContent(Project project) {
        this.project = project;
        listDataObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                if (arg != null && arg instanceof List) {
                    dataModel.arrayList = (List<CommitInfo>) arg;
                    reviewList.clearSelection();
                    reviewList.updateUI();
                } else if (arg != null && arg instanceof String && arg.equals("refreshSub")) {
                    if (changesBrowserBase != null) {
                        reviewList.updateUI();
                        changesBrowserBase.updateUI();
                    }
                }
            }
        };
        CommitListObservable.getInstance(project).addObserver(listDataObserver);
    }

    @Override
    public void disposeContent() {
        CommitListObservable.getInstance(project).deleteObserver(listDataObserver);
    }

    public JComponent initContent() {
        horizontalSplitter = new JBSplitter(false, 0.7f);
        verticalSplitter = new JBSplitter(true, 0.7f);
        SimpleToolWindowPanel basePan = new SimpleToolWindowPanel(true, true);
        ActionToolbar actionToolbar = getToolBar(project);
        actionToolbar.setTargetComponent(reviewList);
        JComponent component = actionToolbar.getComponent();
        basePan.setToolbar(component);
        basePan.setContent(panel1);
        dataModel = new DataModel();
        reviewList.setModel(dataModel);
        reviewList.setCellRenderer(new MRCellRender());
        reviewList.setSelectionBackground(Color.decode("0x4B6EAF"));
        reviewList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() == 1) {
                    int clickCount = e.getClickCount();
                    if (clickCount == 2) {
                        CommitInfo elementAt = dataModel.getElementAt(reviewList.getSelectedIndex());
                        if (elementAt.review_comment != null && !elementAt.review_comment.isEmpty()) {
                            CommentAction.showCommentDialog(project, elementAt);
                        }
                    }
                    return;
                }
                if (e.getButton() == 3) {
                    int clickCount = e.getClickCount();
                    if (clickCount == 1) {
                        reviewList.setSelectedIndex(reviewList.locationToIndex(e.getPoint()));
                        buildMenu(e.getPoint());
                    }
                    return;
                }
            }
        });
        reviewList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && reviewList.getSelectedIndex() >= 0) {
                    CommitInfo selected = dataModel.getElementAt(reviewList.getSelectedIndex());
                    try {
                        acceptAction.setData(selected);
                        commentAction.setData(selected);
                        List<VcsRoot> vcsRoots = Arrays.asList(ProjectLevelVcsManager.getInstance(project).getAllVcsRoots());
                        VcsLogManager manager = new VcsLogManager(project, ServiceManager.getService(project, VcsLogTabsProperties.class), vcsRoots);
                        Map<VirtualFile, VcsLogProvider> logProviders = VcsLogManager.findLogProviders(vcsRoots, project);
                        ArrayList<String> strings = new ArrayList<>();
                        for (VirtualFile virtualFile : logProviders.keySet()) {
                            strings.clear();
                            strings.add(selected.version_hash);
                            VcsLogProvider vcsLogProvider = logProviders.get(virtualFile);
                            vcsLogProvider.readFullDetails(virtualFile, strings, new Consumer<VcsFullCommitDetails>() {
                                @Override
                                public void consume(VcsFullCommitDetails vcsFullCommitDetails) {
                                    if (commitPanel == null) {
                                        VcsLogColorManagerImpl vcsLogColorManager = new VcsLogColorManagerImpl(logProviders.keySet());
                                        vcsLogData = manager.getDataManager();
                                        commitPanel = new CommitPanel(vcsLogData, vcsLogColorManager);
                                        verticalSplitter.setSecondComponent(commitPanel);
                                    }
                                    List<Change> changes = new ArrayList<Change>(vcsFullCommitDetails.getChanges());
                                    ((MyRepositoryChangesBrowser) changesBrowserBase).setChangesToDisplay(changes);
                                    commitPanel.setCommit(vcsFullCommitDetails);
                                    commitPanel.update();
                                }
                            });
                        }
                    } catch (VcsException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        horizontalSplitter.setFirstComponent(basePan);
        changesBrowserBase = new MyRepositoryChangesBrowser(project, null);
        //评论
        commentAction = new CommentAction();
        changesBrowserBase.addToolbarAction(commentAction);
        //reviewed
        acceptAction = new AcceptAction();
        changesBrowserBase.addToolbarAction(acceptAction);

        verticalSplitter.setFirstComponent(changesBrowserBase);
        horizontalSplitter.setSecondComponent(verticalSplitter);
        return horizontalSplitter;
    }


    private ActionToolbar getToolBar(Project project) {
        DefaultActionGroup toolBarActionGroup = (DefaultActionGroup) ActionManager.getInstance().getAction("cn.kuwo.plugin.gitSimpleReview.Toolbar");
        SearchFieldAction searchFieldAction = new SearchFieldAction("") {
            @Override
            public void actionPerformed(AnActionEvent event) {
                CommitFilter.getInstance().searchKey = getText().trim();
                CommitListObservable.getInstance(project).filt();
            }
        };
        toolBarActionGroup.add(searchFieldAction);
//        状态
        StatusPopupAction statusPopupAction = new StatusPopupAction(project, "Status");
        toolBarActionGroup.add(statusPopupAction);
//        检查者
        ReviewerPopupAction reviewerPopupAction = new ReviewerPopupAction(project, "Reviewer");
        toolBarActionGroup.add(reviewerPopupAction);
//        发起者
        OwnerPopupAction ownerPopupAction = new OwnerPopupAction(project, "Owner");
        toolBarActionGroup.add(ownerPopupAction);
        // 分支
        BranchPopupAction branchPopupAction = new BranchPopupAction(project, "Branch");
        toolBarActionGroup.add(branchPopupAction);
        //分割线
        toolBarActionGroup.addSeparator();
        //日期
        DatePopupAction date = new DatePopupAction(project, "Cimmit Date");
        toolBarActionGroup.add(date);
//        刷新
        RefreshAction refreshAction = new RefreshAction();
        toolBarActionGroup.add(refreshAction);
        //分割线
        toolBarActionGroup.addSeparator();
        //设置
        SettingAction settingAction = new SettingAction();
        toolBarActionGroup.add(settingAction);
        return ActionManager.getInstance().createActionToolbar("GitMergeRequest.Toolbar", toolBarActionGroup, true);
    }

    public static class VisibilityPredicate implements NotNullFunction<Project, Boolean> {
        @NotNull
        @Override
        public Boolean fun(@NotNull Project project) {
            return ProjectLevelVcsManager.getInstance(project).checkVcsIsActive(GitVcs.NAME);
        }
    }

    public class DataModel extends AbstractListModel {
        private List<CommitInfo> arrayList;

        @Override
        public CommitInfo getElementAt(int index) {
            if (arrayList == null) {
                return null;
            }
            return arrayList.get(index);
        }

        @Override
        public int getSize() {
            return arrayList == null ? 0 : arrayList.size();
        }
    }

    public class MRCellRender extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            CommitInfo commitInfo = value instanceof CommitInfo ? ((CommitInfo) value) : null;
            if (commitInfo != null) {
                JPanel baseCell = new ReviewCell().getBaseCell(commitInfo);
                if (reviewList.getSelectedIndex() == index) {
                    baseCell.setBackground(reviewList.getSelectionBackground());
                }
                return baseCell;
            }
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    }

    protected void buildMenu(Point point) {
        DefaultActionGroup toolBarActionGroup = (DefaultActionGroup) ActionManager.getInstance().getAction("cn.kuwo.plugin.gitSimpleReview.ListMenu");
        ActionPopupMenu popupMenu =
                ((ActionManagerImpl) ActionManager.getInstance())
                        .createActionPopupMenu("", toolBarActionGroup);
        for (AnAction anAction : toolBarActionGroup.getChildActionsOrStubs()) {
            if (anAction instanceof CopyVersionNumAction) {
                CommitInfo selected = dataModel.getElementAt(reviewList.getSelectedIndex());
                ((CopyVersionNumAction) anAction).setReversionNumber(selected.version_hash);
            }
        }
        popupMenu.getComponent().show(reviewList, point.x, point.y);
    }
}
