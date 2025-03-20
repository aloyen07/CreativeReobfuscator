# CreativeReobfuscator v1.0 alpha

A tool for reobfuscating Minecraft classes using MCP and Mojang mappings for Forge.
This tool by default contains 1.16.5 mappings.

Tool assembled specially for Minecraft Server [VitusFox](https://vitusfox.net)

## Usage

To reobfuscate your class, you need load the mappings and put it into `ClassReobfuscator`

Example:
```java
byte[] bytecode = new byte[100]; // Some logic for load bytecode into next variable...
Pair<LoadState, Mapping> pair = Mapping.getReobfuscationMapping("generated_mapping.tsrg"); // File to save mappings or load old pre-generated mappings

System.out.println("Load state: " + pair.getFirst().name());
// CREATED_NEW - Constructed new mapping from official mappings
// LOADED_FROM_FILE - Mapping loaded from file generated_mapping.tsrg

ClassReobfuscator reobfuscator = new ClassReobfuscator(pair.getSecond());
byte[] reobfuscatedBytecode = reobfuscator.reobfuscate(bytes);
// Some logic to save your bytecode
```

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

## Third-Party Licenses

This project uses the following third-party mappings:
- MCP Mappings: [License](THIRD-PARTY-LICENSES/MCP-LICENSE.txt)
- Mojang Mappings: [Client mappings (at header)](src/main/resources/client_mappings.txt) [Server mappings (at header)](src/main/resources/server_mappings.txt)