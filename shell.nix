{ pkgs ? import <nixpkgs> {} }:
pkgs.mkShell {
  nativeBuildInputs = with pkgs; [
    (yarn.override { nodejs = nodejs-16_x; })
    nodejs-16_x
  ];
}
