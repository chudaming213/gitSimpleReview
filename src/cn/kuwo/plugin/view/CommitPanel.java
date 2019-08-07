package cn.kuwo.plugin.view;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.issueLinks.IssueLinkHtmlRenderer;
import com.intellij.openapi.vcs.history.VcsHistoryUtil;
import com.intellij.openapi.vcs.ui.FontUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.UI;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.*;
import com.intellij.vcs.log.Hash;
import com.intellij.vcs.log.VcsFullCommitDetails;
import com.intellij.vcs.log.VcsRef;
import com.intellij.vcs.log.VcsUser;
import com.intellij.vcs.log.data.LoadingDetails;
import com.intellij.vcs.log.data.VcsLogData;
import com.intellij.vcs.log.ui.VcsLogColorManager;
import com.intellij.vcs.log.ui.frame.HtmlTableBuilder;
import com.intellij.vcs.log.ui.frame.ReferencesPanel;
import com.intellij.vcs.log.ui.render.RectanglePainter;
import com.intellij.vcs.log.ui.table.VcsLogGraphTable;
import com.intellij.vcs.log.util.VcsUserUtil;
import git4idea.branch.GitBranchUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class CommitPanel extends JBPanel {
    public static final int BOTTOM_BORDER = 2;
    private static final int REFERENCES_BORDER = 12;
    private static final int TOP_BORDER = 4;
    @NotNull
    private final VcsLogData myLogData;
    @NotNull
    private final ReferencesPanel myBranchesPanel;
    @NotNull
    private final ReferencesPanel myTagsPanel;
    @NotNull
    private final DataPanel myDataPanel;
    @NotNull
    private final BranchesPanel myContainingBranchesPanel;
    @NotNull
    private final RootPanel myRootPanel;
    @NotNull
    private final VcsLogColorManager myColorManager;
    @Nullable
    private VcsFullCommitDetails myCommit;

    public CommitPanel(@NotNull VcsLogData logData, @NotNull VcsLogColorManager colorManager) {
        this.myLogData = logData;
        this.myColorManager = colorManager;
        this.setLayout(new VerticalFlowLayout(0, 0, 0, true, false));
        this.setOpaque(false);
        this.myRootPanel = new RootPanel();
        this.myBranchesPanel = new ReferencesPanel();
        this.myBranchesPanel.setBorder(JBUI.Borders.empty(12, 0, 0, 0));
        this.myTagsPanel = new ReferencesPanel();
        this.myTagsPanel.setBorder(JBUI.Borders.empty(12, 0, 0, 0));
        this.myDataPanel = new DataPanel(this.myLogData.getProject());
        this.myContainingBranchesPanel = new BranchesPanel();
        this.add(this.myRootPanel);
        this.add(this.myDataPanel);
        this.add(this.myBranchesPanel);
        this.add(this.myTagsPanel);
        this.add(this.myContainingBranchesPanel);
        this.setBorder(getDetailsBorder());
    }

    public void setCommit(@NotNull VcsFullCommitDetails commitData) {
        if (!Comparing.equal(this.myCommit, commitData)) {
            if (commitData instanceof LoadingDetails) {
                this.myDataPanel.setData((VcsFullCommitDetails) null);
                this.myRootPanel.setRoot("", (Color) null);
            } else {
                this.myDataPanel.setData(commitData);
                VirtualFile root = commitData.getRoot();
                if (this.myColorManager.isMultipleRoots()) {
                    this.myRootPanel.setRoot(root.getName(), VcsLogGraphTable.getRootBackgroundColor(root, this.myColorManager));
                } else {
                    this.myRootPanel.setRoot("", (Color) null);
                }
            }

            this.myCommit = commitData;
        }

        java.util.List<String> branches = null;
        if (!(commitData instanceof LoadingDetails)) {
            try {
                Collection<String> branches1 = GitBranchUtil.getBranches(this.myLogData.getProject(), commitData.getRoot(), true, true, commitData.getId().asString());
                branches = new ArrayList<>(branches1);
            } catch (VcsException e) {
                e.printStackTrace();
            }
        }

        this.myContainingBranchesPanel.setBranches(branches);
        this.myDataPanel.update();
        this.myContainingBranchesPanel.update();
        this.revalidate();
    }

    public void setRefs(@NotNull Collection<VcsRef> refs) {
        java.util.List<VcsRef> references = this.sortRefs(refs);
        this.myBranchesPanel.setReferences((java.util.List) references.stream().filter((ref) -> {
            return ref.getType().isBranch();
        }).collect(Collectors.toList()));
        this.myTagsPanel.setReferences((java.util.List) references.stream().filter((ref) -> {
            return !ref.getType().isBranch();
        }).collect(Collectors.toList()));
    }

    public void update() {
        this.myDataPanel.update();
        this.myRootPanel.update();
        this.myBranchesPanel.update();
        this.myTagsPanel.update();
        this.myContainingBranchesPanel.update();
    }

    public void updateBranches() {
        if (this.myCommit != null) {
            this.myContainingBranchesPanel.setBranches(this.myLogData.getContainingBranchesGetter().getContainingBranchesFromCache(this.myCommit.getRoot(), (Hash) this.myCommit.getId()));
        } else {
            this.myContainingBranchesPanel.setBranches((java.util.List) null);
        }

        this.myContainingBranchesPanel.update();
    }

    @NotNull
    private java.util.List<VcsRef> sortRefs(@NotNull Collection<VcsRef> refs) {
        VcsRef ref = (VcsRef) ContainerUtil.getFirstItem(refs);
        return ref == null ? ContainerUtil.emptyList() : ContainerUtil.sorted(refs, this.myLogData.getLogProvider(ref.getRoot()).getReferenceManager().getLabelsOrderComparator());
    }

    @NotNull
    public static JBEmptyBorder getDetailsBorder() {
        return JBUI.Borders.empty();
    }

    public Color getBackground() {
        return getCommitDetailsBackground();
    }

    public boolean isExpanded() {
        return this.myContainingBranchesPanel.isExpanded();
    }

    @NotNull
    public static Color getCommitDetailsBackground() {
        return UIUtil.getTableBackground();
    }

    @NotNull
    public static String formatDateTime(long time) {
        return " on " + DateFormatUtil.formatDate(time) + " at " + DateFormatUtil.formatTime(time);
    }

    private static class RootPanel extends JPanel {
        private static final int RIGHT_BORDER = Math.max(UIUtil.getScrollBarWidth(), JBUI.scale(14));
        @NotNull
        private final RectanglePainter myLabelPainter = new RectanglePainter(true) {
            protected Font getLabelFont() {
                return RootPanel.getLabelFont();
            }
        };
        @NotNull
        private String myText = "";
        @NotNull
        private Color myColor = UIUtil.getTableBackground();

        RootPanel() {
            this.setOpaque(false);
        }

        @NotNull
        private static Font getLabelFont() {
            Font font = VcsHistoryUtil.getCommitDetailsFont();
            return font.deriveFont((float) font.getSize() - 2.0F);
        }

        public void setRoot(@NotNull String text, @Nullable Color color) {
            this.myText = text;
            if (!text.isEmpty() && color != null) {
                this.myColor = color;
            } else {
                this.myColor = UIUtil.getTableBackground();
            }

        }

        public void update() {
            this.revalidate();
            this.repaint();
        }

        protected void paintComponent(Graphics g) {
            if (!this.myText.isEmpty()) {
                Dimension painterSize = this.myLabelPainter.calculateSize(this.myText, this.getFontMetrics(getLabelFont()));
                JBScrollPane scrollPane = (JBScrollPane) UIUtil.getParentOfType(JBScrollPane.class, this);
                int width;
                if (scrollPane == null) {
                    width = this.getWidth();
                } else {
                    width = scrollPane.getViewport().getViewRect().x + scrollPane.getWidth();
                }

                this.myLabelPainter.paint((Graphics2D) g, this.myText, width - painterSize.width - RIGHT_BORDER, 0, this.myColor);
            }

        }

        public Color getBackground() {
            return UIUtil.getTableBackground();
        }

        public Dimension getMinimumSize() {
            return this.getPreferredSize();
        }

        public Dimension getPreferredSize() {
            if (this.myText.isEmpty()) {
                return new JBDimension(0, 4);
            } else {
                Dimension size = this.myLabelPainter.calculateSize(this.myText, this.getFontMetrics(getLabelFont()));
                return new Dimension(size.width + JBUI.scale(RIGHT_BORDER), size.height);
            }
        }

        public Dimension getMaximumSize() {
            return this.getPreferredSize();
        }
    }

    private static class BranchesPanel extends HtmlPanel {
        private static final int PER_ROW = 2;
        private static final String LINK_HREF = "show-hide-branches";
        @Nullable
        private java.util.List<String> myBranches;
        private boolean myExpanded = false;

        BranchesPanel() {
            DefaultCaret caret = (DefaultCaret) this.getCaret();
            caret.setUpdatePolicy(1);
            this.setBorder(JBUI.Borders.empty(12, 4, 2, 0));
        }

        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && "show-hide-branches".equals(e.getDescription())) {
                this.myExpanded = !this.myExpanded;
                this.update();
            }

        }

        public void updateUI() {
            super.updateUI();
            this.update();
        }

        void setBranches(@Nullable List<String> branches) {
            if (branches == null) {
                this.myBranches = null;
            } else {
                this.myBranches = branches;
            }

            this.myExpanded = false;
        }

        void update() {
            this.setText("<html><head>" + UIUtil.getCssFontDeclaration(VcsHistoryUtil.getCommitDetailsFont()) + "</head><body>" + this.getBranchesText() + "</body></html>");
            this.revalidate();
            this.repaint();
        }

        @NotNull
        private String getBranchesText() {
            if (this.myBranches == null) {
                return "<i>In branches: loading...</i>";
            } else if (this.myBranches.isEmpty()) {
                return "<i>Not in any branch</i>";
            } else {
                int rowCount;
                if (this.myExpanded) {
                    rowCount = (int) Math.ceil((double) this.myBranches.size() / 2.0D);
                    int[] means = new int[1];
                    int[] max = new int[1];

                    int i;
                    int j;
                    for (j = 0; j < rowCount; ++j) {
                        for (i = 0; i < 1; ++i) {
                            j = rowCount * i + j;
                            if (j < this.myBranches.size()) {
                                means[i] += ((String) this.myBranches.get(j)).length();
                                max[i] = Math.max(((String) this.myBranches.get(j)).length(), max[i]);
                            }
                        }
                    }

                    for (j = 0; j < 1; ++j) {
                        means[j] /= rowCount;
                    }

                    HtmlTableBuilder builder = new HtmlTableBuilder();

                    for (i = 0; i < rowCount; ++i) {
                        builder.startRow();

                        for (j = 0; j < 2; ++j) {
                            int index = rowCount * j + i;
                            if (index >= this.myBranches.size()) {
                                builder.append("");
                            } else {
                                String branch = (String) this.myBranches.get(index);
                                if (index != this.myBranches.size() - 1) {
                                    int space = 0;
                                    if (j < 1 && branch.length() == max[j]) {
                                        space = Math.max(means[j] + 20 - max[j], 5);
                                    }

                                    builder.append(branch + StringUtil.repeat("&nbsp;", space), "left");
                                } else {
                                    builder.append(branch, "left");
                                }
                            }
                        }

                        builder.endRow();
                    }

                    return "<i>In " + this.myBranches.size() + " branches:</i> <a href=\"" + "show-hide-branches" + "\"><i>(click to hide)</i></a><br>" + builder.build();
                } else {
                    rowCount = 0;
                    int charCount = 0;
                    Iterator var3 = this.myBranches.iterator();

                    while (var3.hasNext()) {
                        String b = (String) var3.next();
                        ++rowCount;
                        charCount += b.length();
                        if (charCount >= 50) {
                            break;
                        }
                    }

                    String branchText;
                    if (this.myBranches.size() <= rowCount) {
                        branchText = StringUtil.join(this.myBranches, ", ");
                    } else {
                        branchText = StringUtil.join(ContainerUtil.getFirstItems(this.myBranches, rowCount), ", ") + "… <a href=\"" + "show-hide-branches" + "\"><i>(click to show all)</i></a>";
                    }

                    return "<i>In " + this.myBranches.size() + StringUtil.pluralize(" branch", this.myBranches.size()) + ":</i> " + branchText;
                }
            }
        }

        public Color getBackground() {
            return UIUtil.getTableBackground();
        }

        public boolean isExpanded() {
            return this.myExpanded;
        }
    }

    private static class DataPanel extends HtmlPanel {
        @NotNull
        private final Project myProject;
        @Nullable
        private String myMainText;

        DataPanel(@NotNull Project project) {
            this.myProject = project;
            DefaultCaret caret = (DefaultCaret) this.getCaret();
            caret.setUpdatePolicy(1);
            this.setBorder(JBUI.Borders.empty(0, 4, 2, 0));
        }

        public void updateUI() {
            super.updateUI();
            this.update();
        }

        void setData(@Nullable VcsFullCommitDetails commit) {
            if (commit == null) {
                this.myMainText = null;
            } else {
                String hash = ((Hash) commit.getId()).toShortString();
                String hashAndAuthor = getHtmlWithFonts(hash + " " + getAuthorText(commit, hash.length() + 1));
                String messageText = this.getMessageText(commit);
                this.myMainText = messageText + "<br/><br/>" + hashAndAuthor;
            }

        }

        private void customizeLinksStyle() {
            Document document = this.getDocument();
            if (document instanceof HTMLDocument) {
                StyleSheet styleSheet = ((HTMLDocument) document).getStyleSheet();
                String linkColor = "#" + ColorUtil.toHex(UI.getColor("link.foreground"));
                styleSheet.addRule("a { color: " + linkColor + "; text-decoration: none;}");
            }

        }

        @NotNull
        private static String getHtmlWithFonts(@NotNull String input) {
            return getHtmlWithFonts(input, VcsHistoryUtil.getCommitDetailsFont().getStyle());
        }

        @NotNull
        private static String getHtmlWithFonts(@NotNull String input, int style) {
            return FontUtil.getHtmlWithFonts(input, style, VcsHistoryUtil.getCommitDetailsFont());
        }

        void update() {
            if (this.myMainText == null) {
                this.setText("");
            } else {
                this.setText("<html><head>" + UIUtil.getCssFontDeclaration(VcsHistoryUtil.getCommitDetailsFont()) + "</head><body>" + this.myMainText + "</body></html>");
            }

            this.customizeLinksStyle();
            this.revalidate();
            this.repaint();
        }

        @NotNull
        private String getMessageText(@NotNull VcsFullCommitDetails commit) {
            String fullMessage = commit.getFullMessage();
            int separator = fullMessage.indexOf("\n\n");
            String subject = separator > 0 ? fullMessage.substring(0, separator) : fullMessage;
            String description = fullMessage.substring(subject.length());
            return "<b>" + getHtmlWithFonts(escapeMultipleSpaces(IssueLinkHtmlRenderer.formatTextWithLinks(this.myProject, subject)), 1) + "</b>" + getHtmlWithFonts(escapeMultipleSpaces(IssueLinkHtmlRenderer.formatTextWithLinks(this.myProject, description)));
        }

        @NotNull
        private static String escapeMultipleSpaces(@NotNull String text) {
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < text.length(); ++i) {
                if (text.charAt(i) == ' ') {
                    if (i != text.length() - 1 && text.charAt(i + 1) == ' ') {
                        result.append("&nbsp;");
                    } else {
                        result.append(' ');
                    }
                } else {
                    result.append(text.charAt(i));
                }
            }

            return result.toString();
        }

        @NotNull
        private static String getAuthorText(@NotNull VcsFullCommitDetails commit, int offset) {
            long authorTime = commit.getAuthorTime();
            long commitTime = commit.getCommitTime();
            String authorText = getAuthorName(commit.getAuthor()) + com.intellij.vcs.log.ui.frame.CommitPanel.formatDateTime(authorTime);
            if (!VcsUserUtil.isSamePerson(commit.getAuthor(), commit.getCommitter())) {
                String commitTimeText;
                if (authorTime != commitTime) {
                    commitTimeText = com.intellij.vcs.log.ui.frame.CommitPanel.formatDateTime(commitTime);
                } else {
                    commitTimeText = "";
                }

                authorText = authorText + getCommitterText(commit.getCommitter(), commitTimeText, offset);
            } else if (authorTime != commitTime) {
                authorText = authorText + getCommitterText((VcsUser) null, com.intellij.vcs.log.ui.frame.CommitPanel.formatDateTime(commitTime), offset);
            }

            return authorText;
        }

        @NotNull
        private static String getCommitterText(@Nullable VcsUser committer, @NotNull String commitTimeText, int offset) {
            String alignment = "<br/>" + StringUtil.repeat("&nbsp;", offset);
            String gray = ColorUtil.toHex(JBColor.GRAY);
            String graySpan = "<span style='color:#" + gray + "'>";
            String text = alignment + graySpan + "committed";
            if (committer != null) {
                text = text + " by " + VcsUserUtil.getShortPresentation(committer);
                if (!committer.getEmail().isEmpty()) {
                    text = text + "</span>" + getEmailText(committer) + graySpan;
                }
            }

            text = text + commitTimeText + "</span>";
            return text;
        }

        @NotNull
        private static String getAuthorName(@NotNull VcsUser user) {
            String username = VcsUserUtil.getShortPresentation(user);
            return user.getEmail().isEmpty() ? username : username + getEmailText(user);
        }

        @NotNull
        private static String getEmailText(@NotNull VcsUser user) {
            return " <a href='mailto:" + user.getEmail() + "'>&lt;" + user.getEmail() + "&gt;</a>";
        }

        public Color getBackground() {
            return UIUtil.getTableBackground();
        }
    }
}

