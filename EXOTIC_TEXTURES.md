# Exotic Item Textures

Exotic loot modifiers can select a client-side item model without registering a
new item. Minecraft 1.21.1 uses the integer `minecraft:custom_model_data`
component together with a legacy item-model override. The Executioner loot
modifier sets:

```json
"custom_model_data": 42001
```

The golden-axe model needs a matching override in:

```text
src/main/resources/assets/minecraft/models/item/golden_axe.json
```

The override routes the matching value to the custom model:

```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "layer0": "minecraft:item/golden_axe"
  },
  "overrides": [
    {
      "predicate": {
        "custom_model_data": 42001
      },
      "model": "destinedtraits:item/exotic/the_executioner"
    }
  ]
}
```

That model location resolves to
`src/main/resources/assets/destinedtraits/models/item/exotic/the_executioner.json`.
Its texture is stored at
`src/main/resources/assets/destinedtraits/textures/item/exotic/the_executioner.png`.
The model JSON uses the handheld parent:

```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "layer0": "destinedtraits:item/exotic/the_executioner"
  }
}
```

When adding another exotic texture, assign it a unique `custom_model_data`
integer and add the corresponding predicate to that base item's model override.
Bows, crossbows, shields, and other special-rendered items may need overrides
that preserve their vanilla animation or rendering behavior. Armor worn on an
entity uses separate armor textures.

The field is optional, so existing exotic modifier JSON remains valid and keeps
its normal base-item appearance until a custom model override is configured.