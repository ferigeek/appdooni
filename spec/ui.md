# Structure

- A BorderPane
  - Top: a menu bar with 3 menu items:
    - `File`:
      - Import data
      - Export data
      - Exit
    - `Edit`:
      - Add operating system
      - Modify operating system
      - Delete operating system
      - Adding a tag
      - Modify a tag
      - Delete a tag
    - `About`: 
      - about: shows a tiny window, giving information about the app
      - source code: goes to the GitHub repository
  - Right: Tag section including some sections on top of each other
    - Tag label and add button to add a tag right in front of the label. The button should be the right side of the section, and the label the left side.
    - Search field to search for tags
    - List of tags which you can choose any, one or more, to filter the list of the applications shown.
  - Center: The application section
    - TabPane, tabs on top for each operating system and one tab named "All" at the beginning. the list of the applications should be shown here and filtered out by each tab.
      - If on `All` tab, all the applications should be visible.
      - If on a specific operating system tab, the list should be filtered out based on the corresponding operating system.
      - If some tags are selected from the tag section, the list should be filtered based on both the tab and the tags.
    - a Search bar for application under the TabPane, and add button right in front of the search bar.
      - Search bar left side of the section, and the button right side.
  - Bottom: the log section
    - A label with representing the log section
    - A field that shows the log of the application
  
# Other UI features

- For adding or modifying tags and operating systems, a tiny window should open up to enter the required information
- For removing tags and operating system, a tiny windows should open, showing the list of tags/operating system, and user should be able to remove from them.
- By right-clicking any of the tags in the tag list in the tag section user should be able to see options to remove, or edit one.
- By pressing the add button for application, user should see a tiny window, with required fields to enter.
- By double-clicking any of the applications in the applications list, user should see a tiny window, showing the information of the application.
  - There should be a toggle for editing the information, which is disabled by default, but when enabled, the attributes should be editable.
