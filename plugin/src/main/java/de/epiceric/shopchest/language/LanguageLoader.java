package de.epiceric.shopchest.language;

import de.epiceric.shopchest.ShopChest;
import de.epiceric.shopchest.config.FileLoader;
import de.epiceric.shopchest.config.LanguageConfigurationLoader;
import de.epiceric.shopchest.language.item.DummyItemNameManager;
import de.epiceric.shopchest.language.item.ItemNameManager;
import de.epiceric.shopchest.language.item.LocalizedItemNameManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

public class LanguageLoader {

    private final static String DEFAULT_LOCALE = "en_US";
    private final static String MESSAGES_FILENAME = "messages";
    private final static String ITEMS_FILENAME = "items";

    private final ShopChest shopChestPlugin;
    private final String locale;
    private final Logger logger;
    private final FileLoader fileLoader;
    private final LanguageConfigurationLoader languageConfigurationLoader;

    public LanguageLoader(@NotNull ShopChest shopChestPlugin, @NotNull String locale) {
        this.shopChestPlugin = shopChestPlugin;
        this.locale = locale;
        logger = shopChestPlugin.getLogger();
        fileLoader = new FileLoader();
        languageConfigurationLoader = new LanguageConfigurationLoader();
    }

    @NotNull
    public LanguageManager loadLanguageManager() {
        final MessageRegistry messageRegistry = loadMessageRegistry();
        final ItemNameManager itemNameManager = loadItemNameManager();
        return new LanguageManager(messageRegistry, itemNameManager);
    }

    @NotNull
    private MessageRegistry loadMessageRegistry() {
        final String messageLocalizedFileName = getLocalizedFileName(MESSAGES_FILENAME, locale);
        final String messageSavePath = getSavePath(messageLocalizedFileName);
        final String messageResourcePath = getResourcePath(messageLocalizedFileName);
        final String messageDefaultResourcePath = getResourcePath(
                getLocalizedFileName(MESSAGES_FILENAME, DEFAULT_LOCALE));
        final File messagesFile;
        try {
            messagesFile = fileLoader.loadFile(messageSavePath, shopChestPlugin, messageResourcePath,
                    messageDefaultResourcePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final Map<String, String> storedMessages = languageConfigurationLoader.getTranslations(messagesFile, logger);
        final MessageRegistryLoader messageRegistryLoader = new MessageRegistryLoader(storedMessages);
        final String[] messages = messageRegistryLoader.getMessages();
        return new MessageRegistry(messages, p -> shopChestPlugin.getEconomy().format(p));
    }

    @NotNull
    private ItemNameManager loadItemNameManager() {
        final String itemSavePath = getSavePath(getLocalizedFileName(ITEMS_FILENAME, locale));
        final String itemsResourcePath = getResourcePath(getFullFileName(ITEMS_FILENAME));
        final File itemsFile;
        try {
            itemsFile = fileLoader.loadFile(itemSavePath, shopChestPlugin, itemsResourcePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final Map<String, String> storedItems = languageConfigurationLoader.getTranslations(itemsFile, logger);
        if (storedItems.isEmpty()) {
            logger.warning("You have to configure items language file. Follow the usage section on github");
            return new DummyItemNameManager();
        }
        return new LocalizedItemNameManager(storedItems);
    }

    @NotNull
    private String getFullFileName(@NotNull String fileName) {
        return fileName + ".lang";
    }

    @NotNull
    private String getLocalizedFileName(@NotNull String baseName, @NotNull String locale) {
        return getFullFileName(baseName + "-" + locale);
    }

    @NotNull
    private String getResourcePath(@NotNull String fileName) {
        return "lang/" + fileName;
    }

    @NotNull
    private String getSavePath(@NotNull String fileName) {
        return "lang" + File.separator + fileName;
    }

}
