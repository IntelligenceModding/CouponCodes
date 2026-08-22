package de.doomedartemis.couponcodes.client;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig.Entry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.ListValueSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ValueSpec;
import org.jetbrains.annotations.Nullable;

public class SearchableConfigurationSectionScreen extends ConfigurationScreen.ConfigurationSectionScreen {
    private static final String SECTION = "neoforge.configuration.uitext.section";
    private static final String SECTION_TEXT = "neoforge.configuration.uitext.sectiontext";
    private static final int SEARCH_WIDTH = 310;
    private static final int SEARCH_HEIGHT = 14;
    private static final int SEARCH_GAP = 6;

    private String searchText = "";
    @Nullable
    private EditBox searchBox;
    private int unshiftedListTop;
    private int unshiftedListBottom;

    public SearchableConfigurationSectionScreen(Screen parent, ModConfig.Type type, ModConfig modConfig, Component title) {
        super(parent, type, modConfig, title);
    }

    private SearchableConfigurationSectionScreen(
            Context parentContext,
            Screen parent,
            java.util.Map<String, Object> valueSpecs,
            String key,
            java.util.Set<? extends Entry> entrySet,
            Component title
    ) {
        super(parentContext, parent, valueSpecs, key, entrySet, title);
    }

    @Override
    protected void init() {
        super.init();

        if (list == null) {
            return;
        }

        unshiftedListTop = list.getY();
        unshiftedListBottom = list.getY() + list.getHeight();
        int searchY = unshiftedListTop + font.lineHeight + 2;
        searchBox = new EditBox(font, width / 2 - SEARCH_WIDTH / 2, searchY, SEARCH_WIDTH, SEARCH_HEIGHT, Component.translatable("coupon_codes.configuration.search"));
        searchBox.setCanLoseFocus(true);
        searchBox.setMaxLength(128);
        searchBox.setValue(searchText);
        searchBox.setResponder(value -> {
            searchText = value == null ? "" : value;
            rebuild();
        });
        searchBox.setFocused(false);
        addRenderableWidget(searchBox);
        applySearchLayout();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        applySearchLayout();
        super.render(graphics, mouseX, mouseY, partialTick);
        if (searchBox != null) {
            Component label = Component.translatable("coupon_codes.configuration.search");
            int x = searchBox.getX() + (searchBox.getWidth() - font.width(label)) / 2;
            graphics.drawString(font, label, x, searchBox.getY() - font.lineHeight, 0xFFFFFF, false);
        }
    }

    private void applySearchLayout() {
        if (list == null || searchBox == null) {
            return;
        }

        int searchY = unshiftedListTop + font.lineHeight + 2;
        int shiftedListTop = searchY + SEARCH_HEIGHT + SEARCH_GAP;
        searchBox.setX(width / 2 - SEARCH_WIDTH / 2);
        searchBox.setY(searchY);
        list.setY(shiftedListTop);
        list.setHeight(Math.max(0, unshiftedListBottom - shiftedListTop));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected ConfigurationScreen.ConfigurationSectionScreen rebuild() {
        if (list != null) {
            list.children().clear();
            boolean hasUndoableElements = false;

            final List<@Nullable Element> elements = new ArrayList<>();
            if (isRootGlobalSearch()) {
                collectMatchingElements(context.entries(), context.valueSpecs(), "", elements);
            } else {
                for (final Entry entry : context.entries()) {
                    final String key = entry.getKey();
                    final Object rawValue = entry.getRawValue();
                    Element element = createElement(key, rawValue, context.valueSpecs().get(key));

                    if (matchesSearch(key, element)) {
                        elements.add(element);
                    }
                }
            }
            elements.addAll(filteredSyntheticValues());

            for (final Element element : elements) {
                if (element != null) {
                    if (element.name() == null) {
                        list.addSmall(new StringWidget(Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, Component.empty(), font), element.getWidget(options));
                    } else {
                        final StringWidget label = new StringWidget(Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, element.name(), font).alignLeft();
                        label.setTooltip(Tooltip.create(element.tooltip()));
                        list.addSmall(label, element.getWidget(options));
                    }
                    hasUndoableElements |= element.undoable();
                }
            }

            if (hasUndoableElements && undoButton == null) {
                createUndoButton();
                createResetButton();
            }
        }
        return this;
    }

    @Override
    @Nullable
    protected ValueSpec getValueSpec(final String key) {
        ValueSpec valueSpec = valueSpec(context.valueSpecs(), key);
        return valueSpec != null ? valueSpec : super.getValueSpec(key);
    }

    @Override
    protected String getTranslationKey(final String key) {
        ValueSpec valueSpec = valueSpec(context.valueSpecs(), key);
        if (valueSpec != null && valueSpec.getTranslationKey() != null) {
            return valueSpec.getTranslationKey();
        }
        return super.getTranslationKey(key);
    }

    @Override
    protected String getComment(final String key) {
        ValueSpec valueSpec = valueSpec(context.valueSpecs(), key);
        if (valueSpec != null) {
            return valueSpec.getComment();
        }
        return super.getComment(key);
    }

    @Override
    protected void onChanged(final String key) {
        changed = true;
        ValueSpec valueSpec = getValueSpec(key);
        if (valueSpec != null) {
            needsRestart = needsRestart.with(valueSpec.restartType());
        }
    }

    @Override
    @Nullable
    protected <T> Element createList(final String key, final ListValueSpec spec, final ModConfigSpec.ConfigValue<List<T>> list) {
        return new Element(Component.translatable(SECTION, getTranslationComponent(key)), getTooltipComponent(key, null),
                Button.builder(Component.translatable(SECTION, Component.translatable(getTranslationKey(key) + ".button")),
                        button -> minecraft.setScreen(sectionCache.computeIfAbsent(key,
                                ignored -> new SearchableConfigurationListScreen<>(
                                        Context.list(context, this),
                                        key,
                                        Component.translatable(getTranslationKey(key)),
                                        spec,
                                        list
                                ).rebuilt())))
                        .tooltip(Tooltip.create(getTooltipComponent(key, null)))
                        .width(Button.DEFAULT_WIDTH)
                        .build(),
                false);
    }

    @Override
    @Nullable
    protected Element createSection(final String key, final UnmodifiableConfig subconfig, final UnmodifiableConfig subsection) {
        if (subconfig.isEmpty()) {
            return null;
        }
        return new Element(Component.translatable(SECTION, getTranslationComponent(key)), getTooltipComponent(key, null),
                Button.builder(Component.translatable(SECTION, Component.translatable(getTranslationKey(key) + ".button")),
                        button -> minecraft.setScreen(sectionCache.computeIfAbsent(key,
                                ignored -> new SearchableConfigurationSectionScreen(
                                        context,
                                        this,
                                        subconfig.valueMap(),
                                        key,
                                        subsection.entrySet(),
                                        Component.translatable(getTranslationKey(key))
                                ).rebuild())))
                        .tooltip(Tooltip.create(getTooltipComponent(key, null)))
                        .width(Button.DEFAULT_WIDTH)
                        .build(),
                false);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Nullable
    private Element createElement(String key, Object rawValue, @Nullable Object specValue) {
        Element element = null;

        if (rawValue instanceof ConfigValue cv) {
            ValueSpec valueSpec = getValueSpec(key);
            if (valueSpec instanceof ListValueSpec listValueSpec) {
                element = createList(key, listValueSpec, cv);
            } else if (valueSpec != null && cv.getClass() == ConfigValue.class) {
                Object defaultValue = valueSpec.getDefault();
                if (defaultValue instanceof String) {
                    element = createStringValue(key, valueSpec::test, () -> (String) cv.getRaw(), cv::set);
                } else if (defaultValue instanceof Integer) {
                    element = createIntegerValue(key, valueSpec, () -> (Integer) cv.getRaw(), cv::set);
                } else if (defaultValue instanceof Long) {
                    element = createLongValue(key, valueSpec, () -> (Long) cv.getRaw(), cv::set);
                } else if (defaultValue instanceof Double) {
                    element = createDoubleValue(key, valueSpec, () -> (Double) cv.getRaw(), cv::set);
                } else if (defaultValue instanceof Enum<?>) {
                    element = createEnumValue(key, valueSpec, (Supplier) cv::getRaw, (Consumer) cv::set);
                }
            } else if (valueSpec != null) {
                if (cv instanceof ModConfigSpec.BooleanValue value) {
                    element = createBooleanValue(key, valueSpec, value::getRaw, value::set);
                } else if (cv instanceof ModConfigSpec.IntValue value) {
                    element = createIntegerValue(key, valueSpec, value::getRaw, value::set);
                } else if (cv instanceof ModConfigSpec.LongValue value) {
                    element = createLongValue(key, valueSpec, value::getRaw, value::set);
                } else if (cv instanceof ModConfigSpec.DoubleValue value) {
                    element = createDoubleValue(key, valueSpec, value::getRaw, value::set);
                } else if (cv instanceof ModConfigSpec.EnumValue value) {
                    element = createEnumValue(key, valueSpec, (Supplier) value::getRaw, (Consumer) value::set);
                } else {
                    element = createOtherValue(key, cv);
                }
            }

            return element == null ? null : context.filter().filterEntry(context, key, element);
        }

        if (rawValue instanceof UnmodifiableConfig subsection && specValue instanceof UnmodifiableConfig subconfig) {
            return createSection(key, subconfig, subsection);
        }

        return context.filter().filterEntry(context, key, createOtherSection(key, rawValue));
    }

    private void collectMatchingElements(
            java.util.Set<? extends Entry> entries,
            java.util.Map<String, Object> valueSpecs,
            String prefix,
            List<@Nullable Element> elements
    ) {
        for (Entry entry : entries) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object rawValue = entry.getRawValue();
            Object specValue = valueSpecs.get(entry.getKey());
            if (rawValue instanceof UnmodifiableConfig subsection && specValue instanceof UnmodifiableConfig subconfig) {
                collectMatchingElements(subsection.entrySet(), subconfig.valueMap(), key, elements);
                continue;
            }

            Element element = createElement(key, rawValue, specValue);
            if (matchesSearch(key, element)) {
                elements.add(element);
            }
        }
    }

    private boolean isRootGlobalSearch() {
        return !searchText.isBlank() && context.keylist().isEmpty();
    }

    private Collection<? extends Element> filteredSyntheticValues() {
        if (searchText.isBlank()) {
            return createSyntheticValues();
        }
        return createSyntheticValues().stream()
                .filter(element -> matchesSearch("", element))
                .toList();
    }

    private boolean matchesSearch(String key, @Nullable Element element) {
        String query = searchText.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return true;
        }
        if (element == null) {
            return false;
        }

        String haystack = searchableText(key, element).toLowerCase(Locale.ROOT);
        for (String token : query.split("\\s+")) {
            if (!haystack.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private String searchableText(String key, Element element) {
        StringBuilder text = new StringBuilder(key);
        for (String parentKey : context.keylist()) {
            text.append(' ').append(parentKey);
        }
        append(text, element.name());
        append(text, element.tooltip());
        Object widgetOrOption = element.any();
        if (widgetOrOption instanceof AbstractWidget widget) {
            append(text, widget.getMessage());
        } else if (widgetOrOption instanceof OptionInstance<?> option) {
            text.append(' ').append(option.toString());
        }
        return text.toString();
    }

    private static void append(StringBuilder text, @Nullable Component component) {
        if (component != null) {
            text.append(' ').append(component.getString());
        }
    }

    @Nullable
    private static ValueSpec valueSpec(java.util.Map<String, Object> valueSpecs, String key) {
        Object value = nestedValue(valueSpecs, key);
        return value instanceof ValueSpec valueSpec ? valueSpec : null;
    }

    @Nullable
    private static Object nestedValue(java.util.Map<String, Object> values, String key) {
        String[] path = key.split("\\.");
        Object current = values;
        for (String segment : path) {
            if (current instanceof java.util.Map<?, ?> map) {
                current = map.get(segment);
            } else if (current instanceof UnmodifiableConfig config) {
                current = config.valueMap().get(segment);
            } else {
                return null;
            }
        }
        return current;
    }

    private static final class SearchableConfigurationListScreen<T> extends ConfigurationScreen.ConfigurationListScreen<T> {
        private SearchableConfigurationListScreen(Context context, String key, Component title, ListValueSpec spec, ConfigValue<List<T>> valueList) {
            super(context, key, title, spec, valueList);
        }

        private SearchableConfigurationListScreen<T> rebuilt() {
            rebuild();
            return this;
        }

        @Override
        @Nullable
        protected ValueSpec getValueSpec(final String key) {
            ValueSpec valueSpec = SearchableConfigurationSectionScreen.valueSpec(context.valueSpecs(), key);
            return valueSpec != null ? valueSpec : super.getValueSpec(key);
        }

        @Override
        protected String getTranslationKey(final String key) {
            ValueSpec valueSpec = SearchableConfigurationSectionScreen.valueSpec(context.valueSpecs(), key);
            if (valueSpec != null && valueSpec.getTranslationKey() != null) {
                return valueSpec.getTranslationKey();
            }
            return super.getTranslationKey(key);
        }

        @Override
        protected String getComment(final String key) {
            ValueSpec valueSpec = SearchableConfigurationSectionScreen.valueSpec(context.valueSpecs(), key);
            if (valueSpec != null) {
                return valueSpec.getComment();
            }
            return super.getComment(key);
        }
    }
}
