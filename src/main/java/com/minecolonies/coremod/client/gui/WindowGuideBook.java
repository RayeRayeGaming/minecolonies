package com.minecolonies.coremod.client.gui;

import com.minecolonies.api.util.LanguageHandler;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.blockout.Alignment;
import com.minecolonies.blockout.Pane;
import com.minecolonies.blockout.controls.Button;
import com.minecolonies.blockout.controls.ButtonHandler;
import com.minecolonies.blockout.controls.ButtonImage;
import com.minecolonies.blockout.controls.Text;
import com.minecolonies.blockout.controls.TextField;
import com.minecolonies.blockout.views.ScrollingList;
import com.minecolonies.blockout.views.SwitchView;
import com.minecolonies.blockout.views.View;
import com.minecolonies.blockout.views.Window;

import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumHand;

import java.util.List;
import org.jetbrains.annotations.NotNull;

import static com.minecolonies.api.util.constant.NbtTagConstants.BOOKMARKS;
import static com.minecolonies.api.util.constant.NbtTagConstants.LAST_PAGE;
import static com.minecolonies.api.util.constant.TranslationConstants.*;
import static com.minecolonies.api.util.constant.WindowConstants.*;

public class WindowGuideBook extends Window implements ButtonHandler {

    /*
     * INFORMATION: --> PAGE SYSTEM IS BASED ON NUMBER OF LEFT PAGE,
     * THAT NUMBER DOES NOT MATCH ACTUAL PAGE NUMBER <--
     * pages are in two switchViews (one for each side),
     * number of the current page is an index number in the switchView with left pages (starting with "1")
     *              example: you see page numbers 9 and 10, so you use setPages(5)
     * 
     * ALSO: there is a script to build a content for this book from our wiki,
     * styles for the content are inside the same folder as the script
     */

    private static final String BOOK_RESOURCE = ":gui/guidebook/";

    // Number of pages in pagesLeft - 1 (for the errorPage)
    private final int maxViews;
    // Holds player's guidebook
    private ItemStack playersBook;
    // Holds player's advancements
    private PlayerAdvancements playersAdvancements;
    // List of all indexes
    private String[] indexList;
    // NBT tags to store bookmarks and lastPage
    private NBTTagCompound nbtComp = new NBTTagCompound();
    private NBTTagList bookmarks = new NBTTagList();

    public WindowGuideBook(final ItemStack book, final PlayerAdvancements advancements, final String currentLanguage)
    {
        super(Constants.MOD_ID + BOOK_RESOURCE + currentLanguage + ".xml");
        maxViews = findPaneOfTypeByID(PAGES_LEFT, SwitchView.class).getChildrenSize() - 1;
        playersBook = book;
        playersAdvancements = advancements;

        if (playersBook.hasTagCompound())
        {
            nbtComp = playersBook.getTagCompound();
            bookmarks = nbtComp.getTagList(BOOKMARKS, 8);
        }
    }

    @Override
    public void onButtonClicked(@NotNull final Button button)
    {
        switch (button.getID())
        {
            // Page handlers
            case BUTTON_PREVPAGE:
                setPages(getCurrentPage() - 1);
                return;
            case BUTTON_NEXTPAGE:
                setPages(getCurrentPage() + 1);
                return;
            // Bookmarks management handlers
            case BUTTON_BOOKMARK_CONFIRM:
                final String newBookmark = findPaneOfTypeByID(INPUT_BOOKMARK_NEW_NAME, TextField.class).getText();

                for (int i = 0; i < bookmarks.tagCount(); i++)
                {
                    if (bookmarks.getStringTagAt(i).split(":")[0].equals(newBookmark))
                    {
                        findPaneOfTypeByID(INPUT_BOOKMARK_NEW_NAME, TextField.class)
                                .setTextIgnoreLength(LanguageHandler.format(GUIDEBOOK_BOOKMARK_ALREADY_EXISTS));
                        return;
                    }
                }
                findPaneOfTypeByID(INPUT_BOOKMARK_NEW_NAME, TextField.class).setText("");
                bookmarks.appendTag(new NBTTagString(newBookmark + ":" + getCurrentPage()));

                setPages(getCurrentPage());
                return;
            case BUTTON_BOOKMARK_CANCEL:
                setPages(getCurrentPage());
                return;
            case BUTTON_BOOKMARK_DELETE:
                bookmarks.removeTag(Integer.parseInt(button.getLabel()));
                setPages(getCurrentPage());
                return;
            // Index and bookmark buttons handlers
            case LIST_BOOKMARK + LIST_NAME:
                for (int i = 0; i < bookmarks.tagCount(); i++)
                {
                    if (bookmarks.getStringTagAt(i).split(":")[0].equals(button.getLabel()))
                    {
                        int toPage = Integer.parseInt(bookmarks.getStringTagAt(i).split(":")[1]);

                        if (toPage > maxViews)
                        {
                            errorPage(LanguageHandler.format(GUIDEBOOK_BOOKMARK_PROBABLY_REMOVED, toPage * 2));
                        }
                        else setPages(toPage);

                        return;
                    }
                }
                if (getCurrentPage() != 0)
                {
                    findPaneOfTypeByID(PAGES_RIGHT, SwitchView.class).setView(PAGE_RIGHT + PAGE_BOOKMARK_ADD);
                    findPaneOfTypeByID(INPUT_BOOKMARK_NEW_NAME, TextField.class).setFocus();
                }
                return;
            case LIST_INDEX + LIST_NAME:
                for (int i = 0; i < indexList.length; i++)
                {
                    if (button.getLabel().equals(indexList[i].split(":")[0]))
                    {
                        int toPage = Integer.parseInt(indexList[i].split(":")[1]);

                        if (toPage < 1 || toPage > maxViews)
                        {
                            errorPage("Index not found, page: " + toPage);
                        }
                        else setPages(toPage);

                        return;
                    }
                }
                return;
            // Jump to an error if the ID doesn't match any case
            default:
                errorPage("No handler found for button: " + button.getID());
                return;
        }
    }

    @Override
    public void onOpened()
    {
        final List<Pane> listOfIndexes = findPaneOfTypeByID(LIST_OF_INDEXES, View.class).getChildren();

        indexList = new String[listOfIndexes.size()];
        findPaneOfTypeByID(PAGE_ERROR_HEAD, Text.class).setTextContent(LanguageHandler.format(GUIDEBOOK_ERROR_PAGE));

        // Jump to the lastPage if found
        if (nbtComp.hasKey(LAST_PAGE))
        {
            setPages(nbtComp.getInteger(LAST_PAGE));
        }
        else setPages(1);

        // Load Index, since there is no static list it does repopulating of all children
        findPaneOfTypeByID(LIST_INDEX, ScrollingList.class).setDataProvider(new ScrollingList.DataProvider()
        {
            @Override
            public int getElementCount()
            {
                return listOfIndexes.size();
            }

            @Override
            public void updateElement(final int index, @NotNull final Pane rowPane)
            {
                //indexData[3] = indexName + indexPage + indexAdvancement
                final String[] indexData = ((Text) listOfIndexes.get(index)).getTextContent().split(",");
                final int indexPage = Integer.parseInt(indexData[1]);
                /* ADVANCEMENT CHECK
                if (playersAdvancement.getProgress(<Advancement>).isDone())
                {
                    return;
                }
                */

                indexList[index] = indexData[0] + ":" + indexPage;
                rowPane.findPaneOfTypeByID(LIST_INDEX + LIST_NAME, ButtonImage.class).setLabel(indexData[0]);
                rowPane.findPaneOfTypeByID(LIST_INDEX + LIST_PAGE, Text.class).setTextContent(PAGE_SHORCUT + (2 * indexPage - 1));
            }
        });

        // Load Bookmarks
        findPaneOfTypeByID(LIST_BOOKMARK, ScrollingList.class).setDataProvider(new ScrollingList.DataProvider()
        {
            @Override
            public int getElementCount()
            {
                return bookmarks.tagCount() + 1;
            }

            @Override
            public void updateElement(final int index, @NotNull final Pane rowPane)
            {
                final ButtonImage bookmarkName = rowPane.findPaneOfTypeByID(LIST_BOOKMARK + LIST_NAME, ButtonImage.class);
                final ButtonImage bookmarkDelete = rowPane.findPaneOfTypeByID(BUTTON_BOOKMARK_DELETE, ButtonImage.class);
                final Text bookmarkPage = rowPane.findPaneOfTypeByID(LIST_BOOKMARK + LIST_PAGE, Text.class);

                if (index == bookmarks.tagCount())
                {
                    bookmarkPage.setTextContent("");
                    bookmarkDelete.disable();
                    bookmarkName.setLabel(LanguageHandler.format(GUIDEBOOK_BOOKMARK_NEW));
                    bookmarkName.setTextAlignment(Alignment.MIDDLE);
                    return;
                }
                bookmarkDelete.enable();
                bookmarkDelete.setLabel(Integer.toString(index));
                bookmarkName.setLabel(bookmarks.getStringTagAt(index).split(":")[0]);
                bookmarkName.setTextAlignment(Alignment.MIDDLE_LEFT);
                bookmarkPage.setTextContent(PAGE_SHORCUT + (2 * Integer.parseInt(bookmarks.getStringTagAt(index).split(":")[1]) - 1));
            }
        });
    }

    @Override
    public void onClosed()
    {
        // Save the lastPage and TagCompound
        nbtComp.setInteger(LAST_PAGE, getCurrentPage());
        nbtComp.setTag(BOOKMARKS, bookmarks);
        playersBook.setTagCompound(nbtComp);
    }

    /**
     * Set pages in both switchViews and update page number
     * 
     * @param page Number of page to set, see info
     * @return if fails returns given number
     */
    private void setPages(int page)
    {
        if (page > maxViews)
        {
            page = 1;
        }
        if (page < 1)
        {
            page = maxViews;
        }

        findPaneOfTypeByID(PAGES_LEFT, SwitchView.class).setView(PAGE_LEFT + page);
        findPaneOfTypeByID(PAGES_LEFT_NUM, Text.class).setTextContent(Integer.toString(2 * page - 1));
        // If there is no right page for a left page set right page clear
        if (!findPaneOfTypeByID(PAGES_RIGHT, SwitchView.class).setView(PAGE_RIGHT + page))
        {
            findPaneOfTypeByID(PAGES_RIGHT, SwitchView.class).setView(PAGE_RIGHT + "0");
            if (page == maxViews)
            {
                findPaneOfTypeByID(PAGES_RIGHT_NUM, Text.class).setTextContent("");
                return;
            }
        }
        findPaneOfTypeByID(PAGES_RIGHT_NUM, Text.class).setTextContent(Integer.toString(2 * page));
    }

    private int getCurrentPage()
    {
        return Integer.parseInt(findPaneOfTypeByID(PAGES_LEFT, SwitchView.class).getCurrentView().getID().substring(1));
    }

    private void errorPage(String error)
    {
        findPaneOfTypeByID(PAGES_LEFT, SwitchView.class).setView(PAGE_LEFT + "0");
        findPaneOfTypeByID(PAGES_RIGHT, SwitchView.class).setView(PAGE_RIGHT + "0");
        findPaneOfTypeByID(PAGES_LEFT_NUM, Text.class).setTextContent("");
        findPaneOfTypeByID(PAGES_RIGHT_NUM, Text.class).setTextContent("");
        findPaneOfTypeByID(PAGE_ERROR_CONTENT, Text.class).setTextContent(error);
    }
}